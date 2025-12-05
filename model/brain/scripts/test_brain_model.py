#!/usr/bin/env python3
"""
Brain LLM 测试脚本
测试工具路由决策能力
"""

from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel
import torch
import json
import re

# 路径配置
BASE_MODEL_PATH = "../models/Qwen/Qwen2.5-0.5B-Instruct"
LORA_PATH = "output/Qwen2.5-0.5B-Brain/lora/sft"

# System Prompt (与训练数据一致)
SYSTEM_PROMPT = """你是一个智能心理咨询助手的决策大脑(Brain LLM)。你的职责是根据用户输入，判断需要调用哪些工具。

🔧 可用工具：

1. local_chat（必须调用）
   - 描述：生成对话回复，提供情感支持和建议
   - 参数：{"user_input": "用户的原始输入"}
   - 说明：每次都必须调用，是与用户交互的主要方式

2. psychological_assessment（按需调用）
   - 描述：当用户表达严重心理困扰时，进行专业评估
   - 参数：{"trigger_reason": "触发评估的具体原因"}
   - 触发条件：焦虑、抑郁、失眠（持续性）、自杀/自伤倾向、强迫行为等

3. memory_query（按需调用）
   - 描述：查询历史对话记录
   - 参数：{"query": "查询关键词"}
   - 触发条件：用户明确询问历史对话内容

📋 输出格式（严格遵守）：
```json
{
  "tools": [
    {"name": "local_chat", "parameters": {"user_input": "..."}},
    {"name": "psychological_assessment", "parameters": {"trigger_reason": "..."}}
  ]
}
```

⚠️ 重要规则：
1. tools 数组中必须包含 local_chat
2. 根据需要添加其他工具（0-2个）
3. 只输出 JSON，不要额外解释"""


def extract_json(text):
    """从输出中提取 JSON"""
    # 尝试从 markdown 代码块中提取
    match = re.search(r'```json\s*(.*?)\s*```', text, re.DOTALL)
    if match:
        return match.group(1).strip()
    # 尝试直接匹配 JSON
    match = re.search(r'\{.*\}', text, re.DOTALL)
    if match:
        return match.group(0)
    return None


def load_model():
    """加载模型"""
    print("正在加载基座模型...")
    tokenizer = AutoTokenizer.from_pretrained(BASE_MODEL_PATH, trust_remote_code=True)
    model = AutoModelForCausalLM.from_pretrained(
        BASE_MODEL_PATH,
        device_map="auto",
        trust_remote_code=True,
        torch_dtype=torch.float16
    )
    
    print("正在加载 LoRA 权重...")
    model = PeftModel.from_pretrained(model, LORA_PATH)
    
    return model, tokenizer


def brain_decide(model, tokenizer, user_input):
    """调用 Brain LLM 进行决策"""
    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": user_input}
    ]
    
    text = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    model_inputs = tokenizer([text], return_tensors="pt").to(model.device)
    
    with torch.no_grad():
        generated_ids = model.generate(
            model_inputs.input_ids,
            max_new_tokens=128,
            temperature=0.1,  # 低温度保证输出稳定
            do_sample=True,
            top_p=0.9,
            pad_token_id=tokenizer.pad_token_id,
            eos_token_id=tokenizer.eos_token_id,
        )
    
    # 只保留新生成的 token
    generated_ids = generated_ids[0][len(model_inputs.input_ids[0]):]
    response = tokenizer.decode(generated_ids, skip_special_tokens=True)
    
    return response


def test_brain():
    """测试 Brain LLM"""
    model, tokenizer = load_model()
    
    # 测试用例 - 期望工具列表
    test_cases = [
        # 仅 local_chat 场景
        ("你好", ["local_chat"]),
        ("今天天气真不错", ["local_chat"]),
        ("最近有点小烦恼", ["local_chat"]),
        ("有什么好看的书推荐吗", ["local_chat"]),
        ("谢谢你的建议", ["local_chat"]),
        
        # local_chat + psychological_assessment 场景
        ("我最近总是睡不着，已经一周了", ["local_chat", "psychological_assessment"]),
        ("我感觉活着没什么意思", ["local_chat", "psychological_assessment"]),
        ("我每天都很焦虑，心慌", ["local_chat", "psychological_assessment"]),
        ("我想自杀", ["local_chat", "psychological_assessment"]),
        ("我控制不住地哭泣", ["local_chat", "psychological_assessment"]),
        
        # local_chat + memory_query 场景
        ("上次我们聊了什么", ["local_chat", "memory_query"]),
        ("我之前说过什么吗", ["local_chat", "memory_query"]),
        ("你还记得我之前提到的问题吗", ["local_chat", "memory_query"]),
        ("帮我回忆一下上周的对话", ["local_chat", "memory_query"]),
    ]
    
    print("\n" + "=" * 70)
    print("🧠 Brain LLM 工具路由决策测试")
    print("=" * 70)
    
    correct = 0
    total = len(test_cases)
    
    for user_input, expected_tools in test_cases:
        print(f"\n📥 用户输入: {user_input}")
        print(f"   期望工具: {expected_tools}")
        
        response = brain_decide(model, tokenizer, user_input)
        print(f"   模型输出: {response.strip()}")
        
        # 解析 JSON
        json_str = extract_json(response)
        if json_str:
            try:
                result = json.loads(json_str)
                tools_list = result.get("tools", [])
                actual_tools = [t.get("name", "") for t in tools_list if isinstance(t, dict)]
                
                # 检查是否匹配
                if set(actual_tools) == set(expected_tools):
                    print(f"   ✅ 正确! 调用工具: {actual_tools}")
                    correct += 1
                else:
                    print(f"   ❌ 错误! 实际调用: {actual_tools}, 期望: {expected_tools}")
            except json.JSONDecodeError:
                print(f"   ❌ JSON 解析失败")
        else:
            print(f"   ❌ 未找到 JSON 输出")
    
    print("\n" + "=" * 70)
    print(f"📊 测试结果: {correct}/{total} ({correct/total*100:.1f}%)")
    print("=" * 70)


if __name__ == "__main__":
    test_brain()

