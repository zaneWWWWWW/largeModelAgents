#!/usr/bin/env python3
"""
测试量化后的模型性能和推理速度
"""

import os
import sys
import json
import time
import argparse
from pathlib import Path
from datetime import datetime

# 添加LLaMA-Factory到路径
LLF_PY = os.path.join(os.path.dirname(__file__), '..', 'LLaMA-Factory-main')
sys.path.insert(0, os.path.join(LLF_PY, 'src'))

def test_merged_model(model_path, test_cases=None):
    """测试合并后的PyTorch模型"""
    print("\n" + "="*60)
    print("测试合并模型 (PyTorch)")
    print("="*60)
    
    try:
        from transformers import AutoTokenizer, AutoModelForCausalLM
        import torch
        
        device = "cuda" if torch.cuda.is_available() else "cpu"
        print(f"使用设备: {device}")
        
        # 加载模型和分词器
        print(f"加载模型: {model_path}")
        tokenizer = AutoTokenizer.from_pretrained(model_path, trust_remote_code=True)
        model = AutoModelForCausalLM.from_pretrained(
            model_path,
            trust_remote_code=True,
            torch_dtype=torch.float16 if device == "cuda" else torch.float32,
            device_map="auto"
        )
        model.eval()
        
        if test_cases is None:
            test_cases = [
                "我最近感到很沮丧，不知道该怎么办。",
                "考试压力很大，失眠了好几天。",
                "与室友关系紧张，感到很孤独。"
            ]
        
        results = []
        
        for i, prompt in enumerate(test_cases, 1):
            print(f"\n测试用例 {i}: {prompt[:50]}...")
            
            # 准备输入
            messages = [{"role": "user", "content": prompt}]
            text = tokenizer.apply_chat_template(
                messages,
                tokenize=False,
                add_generation_prompt=True
            )
            inputs = tokenizer(text, return_tensors="pt").to(device)
            
            # 推理
            start_time = time.time()
            with torch.no_grad():
                outputs = model.generate(
                    **inputs,
                    max_new_tokens=256,
                    temperature=0.7,
                    top_p=0.9,
                    do_sample=True
                )
            inference_time = time.time() - start_time
            
            # 解码输出
            response = tokenizer.decode(outputs[0], skip_special_tokens=True)
            
            # 提取助手回复
            if "assistant" in response:
                response = response.split("assistant")[-1].strip()
            
            result = {
                "case": i,
                "prompt": prompt,
                "response": response[:200] + "..." if len(response) > 200 else response,
                "inference_time": f"{inference_time:.2f}s",
                "tokens_per_second": f"{len(outputs[0]) / inference_time:.2f}"
            }
            results.append(result)
            
            print(f"回复: {response[:100]}...")
            print(f"推理时间: {inference_time:.2f}s")
        
        return {
            "model_type": "pytorch",
            "model_path": model_path,
            "device": device,
            "results": results
        }
        
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()
        return None


def test_gguf_model(gguf_path, test_cases=None):
    """测试GGUF量化模型"""
    print("\n" + "="*60)
    print("测试GGUF量化模型")
    print("="*60)
    
    try:
        from llama_cpp import Llama
        
        print(f"加载GGUF模型: {gguf_path}")
        
        # 加载GGUF模型
        llm = Llama(
            model_path=gguf_path,
            n_gpu_layers=-1,  # 使用GPU加速
            n_ctx=2048,
            verbose=False
        )
        
        if test_cases is None:
            test_cases = [
                "我最近感到很沮丧，不知道该怎么办。",
                "考试压力很大，失眠了好几天。",
                "与室友关系紧张，感到很孤独。"
            ]
        
        results = []
        
        for i, prompt in enumerate(test_cases, 1):
            print(f"\n测试用例 {i}: {prompt[:50]}...")
            
            # 推理
            start_time = time.time()
            output = llm(
                prompt,
                max_tokens=256,
                temperature=0.7,
                top_p=0.9,
                echo=False
            )
            inference_time = time.time() - start_time
            
            response = output["choices"][0]["text"]
            
            result = {
                "case": i,
                "prompt": prompt,
                "response": response[:200] + "..." if len(response) > 200 else response,
                "inference_time": f"{inference_time:.2f}s",
                "tokens": output.get("usage", {}).get("completion_tokens", "N/A")
            }
            results.append(result)
            
            print(f"回复: {response[:100]}...")
            print(f"推理时间: {inference_time:.2f}s")
        
        return {
            "model_type": "gguf",
            "model_path": gguf_path,
            "results": results
        }
        
    except ImportError:
        print("错误: llama-cpp-python未安装")
        print("请运行: pip install llama-cpp-python")
        return None
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()
        return None


def generate_report(pytorch_results, gguf_results):
    """生成性能对比报告"""
    print("\n" + "="*60)
    print("性能对比报告")
    print("="*60)
    
    report = {
        "timestamp": datetime.now().isoformat(),
        "pytorch_model": pytorch_results,
        "gguf_model": gguf_results,
        "summary": {}
    }
    
    if pytorch_results and gguf_results:
        print("\n模型对比:")
        print(f"PyTorch模型: {pytorch_results['model_path']}")
        print(f"GGUF模型: {gguf_results['model_path']}")
        
        # 计算平均推理时间
        pytorch_times = []
        gguf_times = []
        
        for r in pytorch_results.get("results", []):
            try:
                pytorch_times.append(float(r["inference_time"].rstrip('s')))
            except:
                pass
        
        for r in gguf_results.get("results", []):
            try:
                gguf_times.append(float(r["inference_time"].rstrip('s')))
            except:
                pass
        
        if pytorch_times and gguf_times:
            avg_pytorch = sum(pytorch_times) / len(pytorch_times)
            avg_gguf = sum(gguf_times) / len(gguf_times)
            speedup = avg_pytorch / avg_gguf
            
            print(f"\n平均推理时间:")
            print(f"  PyTorch: {avg_pytorch:.2f}s")
            print(f"  GGUF (Q4_KM): {avg_gguf:.2f}s")
            print(f"  加速比: {speedup:.2f}x")
            
            report["summary"] = {
                "pytorch_avg_time": f"{avg_pytorch:.2f}s",
                "gguf_avg_time": f"{avg_gguf:.2f}s",
                "speedup": f"{speedup:.2f}x"
            }
    
    return report


def main():
    parser = argparse.ArgumentParser(description="测试量化模型")
    parser.add_argument("--merged-model", type=str, 
                       default="models/mindchat-qwen2-05b-merged",
                       help="合并模型路径")
    parser.add_argument("--gguf-model", type=str,
                       default="models/mindchat-qwen2-05b-merged-gguf/model-q4_km.gguf",
                       help="GGUF模型路径")
    parser.add_argument("--output", type=str,
                       default="results/model_test_report.json",
                       help="输出报告路径")
    parser.add_argument("--pytorch-only", action="store_true",
                       help="仅测试PyTorch模型")
    parser.add_argument("--gguf-only", action="store_true",
                       help="仅测试GGUF模型")
    
    args = parser.parse_args()
    
    # 创建输出目录
    os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
    
    pytorch_results = None
    gguf_results = None
    
    # 测试PyTorch模型
    if not args.gguf_only and os.path.exists(args.merged_model):
        pytorch_results = test_merged_model(args.merged_model)
    
    # 测试GGUF模型
    if not args.pytorch_only and os.path.exists(args.gguf_model):
        gguf_results = test_gguf_model(args.gguf_model)
    
    # 生成报告
    report = generate_report(pytorch_results, gguf_results)
    
    # 保存报告
    with open(args.output, 'w', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    
    print(f"\n✓ 报告已保存: {args.output}")
    
    return 0 if (pytorch_results or gguf_results) else 1


if __name__ == "__main__":
    sys.exit(main())

