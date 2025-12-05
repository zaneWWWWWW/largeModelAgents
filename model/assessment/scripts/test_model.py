from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel
import torch
import json

# 路径配置
base_model_path = "models/Qwen/Qwen2___5-0___5B-Instruct"
lora_path = "saves/Qwen2.5-0.5B/lora/sft"

print("正在加载基座模型...")
tokenizer = AutoTokenizer.from_pretrained(base_model_path, trust_remote_code=True)
model = AutoModelForCausalLM.from_pretrained(
    base_model_path, 
    device_map="auto", 
    trust_remote_code=True, 
    torch_dtype=torch.float16
)

print("正在加载 LoRA 权重...")
model = PeftModel.from_pretrained(model, lora_path)

# 读取提示词
with open('data/min_instruction.txt', 'r', encoding='utf-8') as f:
    instruction_text = f.read().strip()

def analyze_psychology(conversation_text):
    messages = [
        {"role": "system", "content": "You are Qwen, created by Alibaba Cloud. You are a helpful assistant."},
        {"role": "user", "content": f"{instruction_text}\n\n对话内容：\n{conversation_text}"}
    ]
    
    # 应用对话模板
    text = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    model_inputs = tokenizer([text], return_tensors="pt").to(model.device)

    generated_ids = model.generate(
        model_inputs.input_ids,
        max_new_tokens=256,
        temperature=0.3,
        do_sample=True,
        top_p=0.8
    )
    
    # 只保留新生成的 token
    generated_ids = [
        output_ids[len(input_ids):] for input_ids, output_ids in zip(model_inputs.input_ids, generated_ids)
    ]

    response = tokenizer.batch_decode(generated_ids, skip_special_tokens=True)[0]
    return response

# 测试用例 1：焦虑 + 学业压力
test_case_1 = """用户: 最近考试压力很大，晚上总是睡不着，心里很焦虑。
咨询师: 可以先调整作息，并做放松训练。我们一起梳理压力来源。
用户: 主要是绩点要求很高，我担心挂科影响保研。
咨询师: 理解你的担忧。先把任务分解，安排复习计划，并保持适度运动。"""

# 测试用例 2：抑郁倾向
test_case_2 = """用户: 最近总是失眠，感觉活着没意思，什么都不想做。
用户: 朋友叫我我也不想理，就想一个人待着。
用户: 以前喜欢的游戏现在也不想碰了，感觉整个人是空的。"""

# 测试用例 3：自杀风险
test_case_3 = """用户: 我觉得我活着没有意义，每天都很痛苦。
用户: 有时候真的想一了百了，结束这一切。
用户: 我已经写好遗书了，只是还没有勇气。"""

print("\n" + "="*60)
print("测试用例 1：焦虑 + 学业压力")
print("="*60)
result_1 = analyze_psychology(test_case_1)
print(f"模型输出：\n{result_1}")
try:
    parsed = json.loads(result_1)
    print(f"✅ JSON 解析成功：{parsed}")
except:
    print("❌ JSON 解析失败")

print("\n" + "="*60)
print("测试用例 2：抑郁倾向")
print("="*60)
result_2 = analyze_psychology(test_case_2)
print(f"模型输出：\n{result_2}")
try:
    parsed = json.loads(result_2)
    print(f"✅ JSON 解析成功：{parsed}")
except:
    print("❌ JSON 解析失败")

print("\n" + "="*60)
print("测试用例 3：自杀风险")
print("="*60)
result_3 = analyze_psychology(test_case_3)
print(f"模型输出：\n{result_3}")
try:
    parsed = json.loads(result_3)
    print(f"✅ JSON 解析成功：{parsed}")
    if parsed.get('risk_flag') == 'suicidal':
        print("⚠️  模型正确识别出自杀风险！")
except:
    print("❌ JSON 解析失败")



