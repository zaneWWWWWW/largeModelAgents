#!/usr/bin/env python3
"""
重构 Brain LLM 训练数据
新架构：local_chat 始终调用 + 可选其他工具
"""

import json
import random
from typing import List, Dict

# 新的 System Prompt
NEW_SYSTEM_PROMPT = """你是一个智能心理咨询助手的决策大脑(Brain LLM)。你的职责是根据用户输入，判断需要调用哪些工具。

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


def convert_single_tool_to_multi(old_sample: Dict) -> Dict:
    """将单工具决策转换为多工具决策"""
    conversations = old_sample.get("conversations", [])
    new_conversations = []
    
    for conv in conversations:
        if conv["from"] == "system":
            # 替换为新的 system prompt
            new_conversations.append({
                "from": "system",
                "value": NEW_SYSTEM_PROMPT
            })
        elif conv["from"] == "user":
            new_conversations.append(conv)
        elif conv["from"] == "assistant":
            # 解析原始工具调用
            old_value = conv["value"]
            
            # 提取 JSON
            try:
                import re
                json_match = re.search(r'\{.*?\}', old_value, re.DOTALL)
                if json_match:
                    old_tool_call = json.loads(json_match.group(0))
                    tool_name = old_tool_call.get("tool", "")
                    parameters = old_tool_call.get("parameters", {})
                    
                    # 构建新的多工具调用
                    tools = []
                    
                    # 1. 必须包含 local_chat
                    if tool_name == "local_chat":
                        tools.append({
                            "name": "local_chat",
                            "parameters": parameters
                        })
                    elif tool_name == "memory_query":
                        # memory_query 场景：先查询，再聊天
                        tools.append({
                            "name": "local_chat",
                            "parameters": {"user_input": parameters.get("query", "")}
                        })
                        tools.append({
                            "name": "memory_query",
                            "parameters": parameters
                        })
                    elif tool_name == "psychological_assessment":
                        # 评估场景：先聊天表示关心，再评估
                        user_input = ""
                        for c in new_conversations:
                            if c["from"] == "user":
                                user_input = c["value"]
                        
                        tools.append({
                            "name": "local_chat",
                            "parameters": {"user_input": user_input}
                        })
                        tools.append({
                            "name": "psychological_assessment",
                            "parameters": parameters
                        })
                    
                    # 生成新的输出
                    new_output = {
                        "tools": tools
                    }
                    
                    new_conversations.append({
                        "from": "assistant",
                        "value": f"```json\n{json.dumps(new_output, ensure_ascii=False, indent=2)}\n```"
                    })
                else:
                    # 无法解析，保留原样
                    new_conversations.append(conv)
            except Exception as e:
                print(f"转换失败: {e}, 保留原样")
                new_conversations.append(conv)
        else:
            # tool 或其他角色
            new_conversations.append(conv)
    
    return {"conversations": new_conversations}


def create_sample_from_psychology_data(psych_data: Dict) -> Dict:
    """从心理咨询数据创建训练样本"""
    user_input = psych_data.get("input", "").strip()
    if not user_input or len(user_input) < 10:
        return None
    
    # 提取第一句学生的话作为简短输入
    lines = user_input.split('\n')
    student_lines = [l.replace("学生: ", "").strip() for l in lines if l.startswith("学生:")]
    
    if not student_lines:
        return None
    
    first_input = student_lines[0]
    
    # 判断是否需要评估
    assessment_keywords = [
        "自杀", "死", "活着没意思", "痛苦", "焦虑", "抑郁", "失眠", 
        "睡不着", "压力大", "崩溃", "绝望", "害怕", "恐惧", "心慌"
    ]
    
    needs_assessment = any(kw in first_input for kw in assessment_keywords)
    
    tools = [
        {
            "name": "local_chat",
            "parameters": {"user_input": first_input}
        }
    ]
    
    if needs_assessment:
        # 提取触发原因
        trigger = "用户表达心理困扰"
        if "自杀" in first_input or "死" in first_input or "活着没意思" in first_input:
            trigger = "用户表达自杀意念或生活无意义感"
        elif "焦虑" in first_input or "心慌" in first_input:
            trigger = "用户表达焦虑症状"
        elif "失眠" in first_input or "睡不着" in first_input:
            trigger = "用户表达严重睡眠问题"
        elif "抑郁" in first_input or "低落" in first_input:
            trigger = "用户表达抑郁情绪"
        
        tools.append({
            "name": "psychological_assessment",
            "parameters": {"trigger_reason": trigger}
        })
    
    output = {"tools": tools}
    
    return {
        "conversations": [
            {"from": "system", "value": NEW_SYSTEM_PROMPT},
            {"from": "user", "value": first_input},
            {"from": "assistant", "value": f"```json\n{json.dumps(output, ensure_ascii=False, indent=2)}\n```"}
        ]
    }


def create_sample_from_alpaca(alpaca_data: Dict) -> Dict:
    """从 Alpaca 数据创建训练样本（仅 local_chat）"""
    instruction = alpaca_data.get("instruction", "").strip()
    input_text = alpaca_data.get("input", "").strip()
    
    if not instruction:
        return None
    
    user_input = f"{instruction}\n{input_text}".strip() if input_text else instruction
    
    # 限制长度
    if len(user_input) > 100:
        user_input = user_input[:100]
    
    tools = [
        {
            "name": "local_chat",
            "parameters": {"user_input": user_input}
        }
    ]
    
    output = {"tools": tools}
    
    return {
        "conversations": [
            {"from": "system", "value": NEW_SYSTEM_PROMPT},
            {"from": "user", "value": user_input},
            {"from": "assistant", "value": f"```json\n{json.dumps(output, ensure_ascii=False, indent=2)}\n```"}
        ]
    }


def main():
    print("=" * 60)
    print("🔄 重构 Brain LLM 训练数据")
    print("=" * 60)
    
    # 1. 转换现有 52 条样本
    print("\n1️⃣ 转换现有 52 条样本...")
    with open("data/brain_training_data.json", "r", encoding="utf-8") as f:
        old_samples = json.load(f)
    
    new_samples = []
    for sample in old_samples:
        converted = convert_single_tool_to_multi(sample)
        new_samples.append(converted)
    
    print(f"   ✅ 转换完成: {len(new_samples)} 条")
    
    # 2. 从心理咨询数据提取
    print("\n2️⃣ 从心理咨询数据提取...")
    with open("LLaMA-Factory/data/psychology_pending.json", "r", encoding="utf-8") as f:
        psych_data = json.load(f)
    
    psych_samples = []
    for item in psych_data:
        sample = create_sample_from_psychology_data(item)
        if sample:
            psych_samples.append(sample)
    
    print(f"   ✅ 提取: {len(psych_samples)} 条")
    
    # 3. 从 Alpaca 数据提取
    print("\n3️⃣ 从 Alpaca 数据提取...")
    with open("LLaMA-Factory/data/alpaca_zh_demo.json", "r", encoding="utf-8") as f:
        alpaca_data = json.load(f)
    
    # 随机抽样
    sampled_alpaca = random.sample(alpaca_data, min(150, len(alpaca_data)))
    
    alpaca_samples = []
    for item in sampled_alpaca:
        sample = create_sample_from_alpaca(item)
        if sample:
            alpaca_samples.append(sample)
    
    print(f"   ✅ 提取: {len(alpaca_samples)} 条")
    
    # 4. 合并并限制到 250 条左右
    print("\n4️⃣ 合并数据...")
    all_samples = new_samples + psych_samples[:100] + alpaca_samples[:100]
    
    # 打乱顺序
    random.shuffle(all_samples)
    
    print(f"   总计: {len(all_samples)} 条")
    
    # 5. 统计
    local_only = 0
    with_assessment = 0
    with_memory = 0
    
    for sample in all_samples:
        for conv in sample["conversations"]:
            if conv["from"] == "assistant":
                if '"psychological_assessment"' in conv["value"]:
                    with_assessment += 1
                elif '"memory_query"' in conv["value"]:
                    with_memory += 1
                else:
                    local_only += 1
    
    print("\n📊 数据分布:")
    print(f"   - 仅 local_chat: {local_only}")
    print(f"   - local_chat + assessment: {with_assessment}")
    print(f"   - local_chat + memory_query: {with_memory}")
    
    # 6. 保存
    output_path = "LLaMA-Factory/data/brain_training_data_v2.json"
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(all_samples, f, ensure_ascii=False, indent=2)
    
    print(f"\n✅ 保存到: {output_path}")
    print("=" * 60)


if __name__ == "__main__":
    main()

