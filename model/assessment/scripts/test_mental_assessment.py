#!/usr/bin/env python3
"""
测试心理评估模型 - 验证JSON输出格式
"""

import os
import sys
import json
import time
import re
from pathlib import Path

# 导入JSON后处理器
sys.path.insert(0, os.path.dirname(__file__))
from json_postprocessor import JSONPostProcessor

try:
    from llama_cpp import Llama
except ImportError:
    print("错误: llama-cpp-python未安装")
    print("请运行: pip install llama-cpp-python")
    sys.exit(1)

# 训练时使用的instruction
INSTRUCTION = "请根据对话内容评估四项标签，并只输出JSON：depression_level(0-3), anxiety_level(0-3), risk_flag(none|suicidal|self_harm|violence), student_distress_score(0-9)。"

def format_conversation(conversation_text):
    """格式化对话内容"""
    return conversation_text

def validate_json_output(text):
    """验证JSON输出格式（使用robust后处理器）"""
    processor = JSONPostProcessor()
    result, error = processor.validate_and_fix(text)
    return result, error

def test_mental_assessment(gguf_path, test_cases=None):
    """测试心理评估模型"""
    print("\n" + "="*60)
    print("测试心理评估模型 - JSON输出验证")
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
            {
                "conversation": "user: 我最近感到很沮丧，不知道该怎么办。\nuser: 学习压力很大，总是担心考试不及格。\nuser: 晚上经常失眠，白天也没有精神。",
                "expected": "应该检测到抑郁和焦虑症状"
            },
            {
                "conversation": "user: 考试压力很大，失眠了好几天。\nuser: 总是担心考不好，影响我的未来。\nuser: 心跳加速，手心出汗。",
                "expected": "应该检测到焦虑症状"
            },
            {
                "conversation": "user: 与室友关系紧张，感到很孤独。\nuser: 不想去上课，也不想和任何人交流。\nuser: 觉得生活没有意义。",
                "expected": "应该检测到抑郁症状"
            },
            {
                "conversation": "user: 最近学习压力很大，总是担心考试不及格。\nuser: 虽然有点紧张，但还能正常学习和生活。",
                "expected": "轻度焦虑，无风险"
            },
            {
                "conversation": "user: 我觉得活着没有意义，想结束这一切。\nuser: 有时候会想伤害自己。",
                "expected": "应该检测到高风险标志"
            }
        ]
    
    results = []
    
    for i, test_case in enumerate(test_cases, 1):
        conversation = test_case["conversation"]
        expected = test_case.get("expected", "")
        
        print(f"\n{'='*60}")
        print(f"测试用例 {i}")
        print('='*60)
        print(f"对话内容: {conversation[:100]}...")
        print(f"预期: {expected}")
        
        # 构建prompt（按照训练时的格式：使用Qwen2的chat template）
        # 训练时：instruction + input 作为user content，使用qwen模板
        user_content = f"{INSTRUCTION}\n\n{conversation}"
        prompt = f"<|im_start|>user\n{user_content}<|im_end|>\n<|im_start|>assistant\n"
        
        try:
            # 推理
            start_time = time.time()
            output = llm(
                prompt,
                max_tokens=128,  # JSON输出通常较短
                temperature=0.0,  # 使用0温度以获得最稳定的输出
                top_p=1.0,
                echo=False,
                stop=["<|im_end|>", "<|endoftext|>"]
            )
            inference_time = time.time() - start_time
            
            raw_response = output["choices"][0]["text"].strip()
            tokens = output.get("usage", {}).get("completion_tokens", 0)
            
            print(f"\n原始输出: {raw_response}")
            
            # 验证JSON格式
            json_result, error = validate_json_output(raw_response)
            
            if json_result:
                print(f"✓ JSON格式正确")
                print(f"评估结果: {json.dumps(json_result, ensure_ascii=False)}")
            else:
                print(f"✗ JSON验证失败: {error}")
            
            result = {
                "case": i,
                "conversation": conversation,
                "expected": expected,
                "raw_response": raw_response,
                "json_result": json_result,
                "validation_error": error,
                "inference_time": f"{inference_time:.3f}s",
                "tokens": tokens,
                "tokens_per_second": f"{tokens / inference_time:.2f}" if inference_time > 0 else "0"
            }
            results.append(result)
            
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
                "conversation": conversation,
                "error": str(e)
            })
    
    # 计算统计信息
    if results:
        valid_json_count = sum(1 for r in results if r.get("json_result") is not None)
        times = [float(r["inference_time"].rstrip('s')) for r in results if "inference_time" in r]
        tokens_list = [r.get("tokens", 0) for r in results if "tokens" in r]
        
        print(f"\n{'='*60}")
        print("统计信息")
        print('='*60)
        print(f"总测试用例: {len(results)}")
        print(f"有效JSON输出: {valid_json_count}/{len(results)} ({valid_json_count/len(results)*100:.1f}%)")
        if times:
            avg_time = sum(times) / len(times)
            total_tokens = sum(tokens_list)
            avg_speed = total_tokens / sum(times) if sum(times) > 0 else 0
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
    output_path = "results/mental_assessment_test.json"
    
    if len(sys.argv) > 1:
        gguf_path = sys.argv[1]
    if len(sys.argv) > 2:
        output_path = sys.argv[2]
    
    # 创建输出目录
    os.makedirs(os.path.dirname(output_path) or ".", exist_ok=True)
    
    # 运行测试
    result = test_mental_assessment(gguf_path)
    
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

