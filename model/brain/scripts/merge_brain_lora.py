#!/usr/bin/env python3
"""
合并 Brain LLM 的 LoRA 权重到基座模型
"""

from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel
import torch
import os

# 路径配置
BASE_MODEL_PATH = "../models/Qwen/Qwen2.5-0.5B-Instruct"
LORA_PATH = "output/Qwen2.5-0.5B-Brain/lora/sft"
OUTPUT_PATH = "output/qwen2.5-0.5b-brain-merged"

def main():
    print("=" * 60)
    print("🧠 合并 Brain LLM LoRA 权重")
    print("=" * 60)
    
    # 加载基座模型
    print("\n1. 加载基座模型...")
    tokenizer = AutoTokenizer.from_pretrained(BASE_MODEL_PATH, trust_remote_code=True)
    model = AutoModelForCausalLM.from_pretrained(
        BASE_MODEL_PATH,
        device_map="auto",
        trust_remote_code=True,
        torch_dtype=torch.float16
    )
    
    # 加载 LoRA 权重
    print("\n2. 加载 LoRA 权重...")
    model = PeftModel.from_pretrained(model, LORA_PATH)
    
    # 合并权重
    print("\n3. 合并权重...")
    model = model.merge_and_unload()
    
    # 保存合并后的模型
    print(f"\n4. 保存合并后的模型到 {OUTPUT_PATH}...")
    os.makedirs(OUTPUT_PATH, exist_ok=True)
    model.save_pretrained(OUTPUT_PATH)
    tokenizer.save_pretrained(OUTPUT_PATH)
    
    print("\n" + "=" * 60)
    print("✅ 合并完成!")
    print(f"   输出路径: {OUTPUT_PATH}")
    print("=" * 60)
    print("\n下一步: 运行 convert_brain_to_gguf.sh 进行量化")


if __name__ == "__main__":
    main()

