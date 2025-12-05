from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel
import torch

# 路径配置
base_model_path = "models/Qwen/Qwen2___5-0___5B-Instruct"
lora_path = "saves/Qwen2.5-0.5B/lora/sft"

print("正在加载基座模型...")
tokenizer = AutoTokenizer.from_pretrained(base_model_path, trust_remote_code=True)
model = AutoModelForCausalLM.from_pretrained(base_model_path, device_map="auto", trust_remote_code=True, torch_dtype=torch.float16)

print("正在加载 LoRA 权重...")
model = PeftModel.from_pretrained(model, lora_path)

def analyze_psychology(text):
    system_prompt = "请根据用户的对话内容，分析其当前的心理状态，并给出简要的心理学解释。"
    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": text}
    ]
    
    # 应用对话模板
    text = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    model_inputs = tokenizer([text], return_tensors="pt").to(model.device)

    generated_ids = model.generate(
        model_inputs.input_ids,
        max_new_tokens=512,
        temperature=0.7,
        do_sample=True
    )
    
    # 只保留新生成的 token
    generated_ids = [
        output_ids[len(input_ids):] for input_ids, output_ids in zip(model_inputs.input_ids, generated_ids)
    ]

    response = tokenizer.batch_decode(generated_ids, skip_special_tokens=True)[0]
    return response

# 测试用例
test_input = "最近总是失眠，感觉活着没意思，什么都不想做，朋友叫我我也不想理。"
print("-" * 30)
print(f"用户输入: {test_input}")
print("-" * 30)
print(f"模型分析: {analyze_psychology(test_input)}")
print("-" * 30)









