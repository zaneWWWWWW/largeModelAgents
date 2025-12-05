#!/usr/bin/env python3
"""
合并 LoRA 权重到基座模型
"""
from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel
import torch
import os

# 路径配置
base_model_path = "models/Qwen/Qwen2___5-0___5B-Instruct"
lora_path = "saves/Qwen2.5-0.5B/lora/sft"
output_path = "output/qwen2.5-0.5b-merged"

print("="*60)
print("开始合并 LoRA 权重到基座模型")
print("="*60)

print(f"\n📁 基座模型路径: {base_model_path}")
print(f"📁 LoRA 权重路径: {lora_path}")
print(f"📁 输出路径: {output_path}")

# 创建输出目录
os.makedirs(output_path, exist_ok=True)

print("\n⏳ 正在加载基座模型...")
tokenizer = AutoTokenizer.from_pretrained(base_model_path, trust_remote_code=True)
model = AutoModelForCausalLM.from_pretrained(
    base_model_path,
    torch_dtype=torch.float16,
    device_map="cpu",  # 使用 CPU 以节省显存
    trust_remote_code=True
)

print("⏳ 正在加载 LoRA 权重...")
model = PeftModel.from_pretrained(model, lora_path)

print("⏳ 正在合并权重...")
model = model.merge_and_unload()

print("⏳ 正在保存合并后的模型...")
model.save_pretrained(output_path, max_shard_size="2GB")
tokenizer.save_pretrained(output_path)

print("\n✅ 模型合并完成！")
print(f"📍 合并后的模型保存在: {output_path}")



