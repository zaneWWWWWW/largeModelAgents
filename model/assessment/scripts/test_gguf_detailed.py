#!/usr/bin/env python3
"""
详细测试GGUF量化模型
"""

import os
import sys
import json
import time
from pathlib import Path

try:
    from llama_cpp import Llama
except ImportError:
    print("错误: llama-cpp-python未安装")
    print("请运行: pip install llama-cpp-python")
    sys.exit(1)

def test_gguf_model(gguf_path, test_cases=None):
    """测试GGUF量化模型"""
    print("\n" + "="*60)
    print("测试GGUF量化模型")
    print("="*60)
    print(f"模型路径: {gguf_path}")
    
    if not os.path.exists(gguf_path):
        print(f"错误: 模型文件不存在: {gguf_path}")
        return None
    
    # 加载GGUF模型
    print("\n加载模型...")
    try:
        llm = Llama(
            model_path=gguf_path,
            n_ctx=2048,
            n_threads=4,
            verbose=False
        )
        print("✓ 模型加载成功")
    except Exception as e:
        print(f"错误: 模型加载失败: {e}")
        return None
    
    if test_cases is None:
        test_cases = [
            "我最近感到很沮丧，不知道该怎么办。",
            "考试压力很大，失眠了好几天。",
            "与室友关系紧张，感到很孤独。",
            "最近学习压力很大，总是担心考试不及格。",
            "我感到很焦虑，不知道如何缓解。"
        ]
    
    results = []
    
    for i, prompt in enumerate(test_cases, 1):
        print(f"\n{'='*60}")
        print(f"测试用例 {i}: {prompt}")
        print('='*60)
        
        try:
            # 使用Qwen2的chat template格式
            formatted_prompt = f"<|im_start|>user\n{prompt}<|im_end|>\n<|im_start|>assistant\n"
            
            # 推理
            start_time = time.time()
            output = llm(
                formatted_prompt,
                max_tokens=256,
                temperature=0.7,
                top_p=0.9,
                echo=False,
                stop=["<|im_end|>", "<|endoftext|>"]
            )
            inference_time = time.time() - start_time
            
            response = output["choices"][0]["text"].strip()
            tokens = output.get("usage", {}).get("completion_tokens", 0)
            
            result = {
                "case": i,
                "prompt": prompt,
                "response": response,
                "inference_time": f"{inference_time:.3f}s",
                "tokens": tokens,
                "tokens_per_second": f"{tokens / inference_time:.2f}" if inference_time > 0 else "0"
            }
            results.append(result)
            
            print(f"回复: {response}")
            print(f"推理时间: {inference_time:.3f}s")
            print(f"生成tokens: {tokens}")
            if inference_time > 0:
                print(f"生成速度: {tokens / inference_time:.2f} tokens/s")
            
        except Exception as e:
            print(f"错误: {e}")
            import traceback
            traceback.print_exc()
            results.append({
                "case": i,
                "prompt": prompt,
                "error": str(e)
            })
    
    # 计算统计信息
    if results:
        times = [float(r["inference_time"].rstrip('s')) for r in results if "inference_time" in r]
        tokens_list = [r.get("tokens", 0) for r in results if "tokens" in r]
        
        if times:
            avg_time = sum(times) / len(times)
            total_tokens = sum(tokens_list)
            avg_speed = total_tokens / sum(times) if sum(times) > 0 else 0
            
            print(f"\n{'='*60}")
            print("统计信息")
            print('='*60)
            print(f"平均推理时间: {avg_time:.3f}s")
            print(f"总生成tokens: {total_tokens}")
            print(f"平均生成速度: {avg_speed:.2f} tokens/s")
    
    return {
        "model_type": "gguf",
        "model_path": gguf_path,
        "results": results
    }


def main():
    gguf_path = "models/mindchat-qwen2-05b-merged-gguf/model-q4_km.gguf"
    output_path = "results/gguf_detailed_test.json"
    
    if len(sys.argv) > 1:
        gguf_path = sys.argv[1]
    if len(sys.argv) > 2:
        output_path = sys.argv[2]
    
    # 创建输出目录
    os.makedirs(os.path.dirname(output_path) or ".", exist_ok=True)
    
    # 运行测试
    result = test_gguf_model(gguf_path)
    
    if result:
        # 保存结果
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
        print(f"\n✓ 测试报告已保存: {output_path}")
        return 0
    else:
        return 1


if __name__ == "__main__":
    sys.exit(main())

