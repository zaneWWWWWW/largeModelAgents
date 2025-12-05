#!/usr/bin/env python3
"""
重构 Brain LLM 训练数据 V2
完全重新生成，不转换旧数据
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


# 手动创建高质量的核心样本
def create_core_samples() -> List[Dict]:
    """创建核心训练样本（覆盖各种场景）"""
    samples = []
    
    # 1. 纯闲聊场景（仅 local_chat）
    chat_samples = [
        "你好",
        "早上好",
        "今天天气真不错",
        "最近有什么好看的电影吗",
        "谢谢你的建议",
        "你说得对，我试试看",
        "和你聊天感觉好多了",
        "最近有点小烦恼",
        "今天工作有点累",
        "周末想去哪里玩呢",
        "你喜欢什么类型的音乐",
        "怎么才能提高学习效率呢",
        "感觉有点无聊",
        "晚上好呀",
    ]
    
    for text in chat_samples:
        samples.append({
            "conversations": [
                {"from": "system", "value": NEW_SYSTEM_PROMPT},
                {"from": "user", "value": text},
                {"from": "assistant", "value": f'```json\n{{\n  "tools": [\n    {{"name": "local_chat", "parameters": {{"user_input": "{text}"}}}}\n  ]\n}}\n```'}
            ]
        })
    
    # 2. 需要评估的场景（local_chat + psychological_assessment）
    assessment_samples = [
        ("我最近压力很大，总是睡不着", "用户表达压力大和失眠"),
        ("我已经连续一周每晚只睡2-3小时", "用户表达严重睡眠障碍"),
        ("我感觉活着没什么意思", "用户表达生活无意义感"),
        ("如果我消失了，是不是大家都会轻松一点", "用户表达自我否定和潜在自杀意念"),
        ("我觉得活着太痛苦了，有时候真想一了百了", "用户表达自杀意念，需紧急评估"),
        ("我最近总是莫名其妙地心慌，感觉很焦虑", "用户表达持续性焦虑和心慌症状"),
        ("我总是感觉很疲惫，即使睡很久也没精神", "用户表达持续疲劳和精力不足"),
        ("我控制不住地反复洗手，每天要洗几十次", "用户表达强迫行为"),
        ("我突然呼吸困难、心跳加速，感觉自己要死了", "用户描述疑似惊恐发作症状"),
        ("我已经连续两周失眠了", "用户表达持续性失眠超过两周"),
        ("最近两周一直很低落，对以前喜欢的事情也提不起兴趣了", "用户表达持续性情绪低落和兴趣丧失"),
        ("我已经写好遗书了", "用户表达自杀计划，极高风险"),
        ("朋友约我出去玩，但我只想一个人待着", "用户表达社交退缩倾向"),
        ("我觉得自己太胖了，已经一周没怎么吃东西了", "用户表达饮食限制行为和身体意象障碍"),
        ("自从那次事故后，我总是做噩梦", "用户表达创伤后应激反应"),
        ("我每天都在担心各种事情，已经影响到我的生活了", "用户表达过度担忧且影响日常功能"),
        ("考试压力太大了，我每晚失眠，心里特别焦虑", "用户表达学业压力导致的焦虑和失眠"),
    ]
    
    for text, reason in assessment_samples:
        samples.append({
            "conversations": [
                {"from": "system", "value": NEW_SYSTEM_PROMPT},
                {"from": "user", "value": text},
                {"from": "assistant", "value": f'```json\n{{\n  "tools": [\n    {{"name": "local_chat", "parameters": {{"user_input": "{text}"}}}},\n    {{"name": "psychological_assessment", "parameters": {{"trigger_reason": "{reason}"}}}}\n  ]\n}}\n```'}
            ]
        })
    
    # 3. 历史查询场景（local_chat + memory_query）
    memory_samples = [
        ("上次我们聊了什么", "上次对话内容"),
        ("查一下我们关于焦虑的对话", "关于焦虑的对话"),
        ("我之前说过什么吗", "历史对话内容"),
        ("你还记得我之前提到的问题吗", "之前提到的问题"),
        ("帮我回忆一下上周的对话", "上周对话"),
        ("我们第一次聊天的时候我说了什么", "第一次聊天内容"),
        ("之前你给我的建议是什么来着", "之前的建议"),
        ("我上次提到的失眠问题，现在怎么样了", "失眠问题"),
        ("你还记得我上次跟你说的事情吗", "上次说的事情"),
        ("我想查一下之前我们聊过的关于焦虑的话题", "关于焦虑的对话"),
    ]
    
    for text, query in memory_samples:
        samples.append({
            "conversations": [
                {"from": "system", "value": NEW_SYSTEM_PROMPT},
                {"from": "user", "value": text},
                {"from": "assistant", "value": f'```json\n{{\n  "tools": [\n    {{"name": "local_chat", "parameters": {{"user_input": "{text}"}}}},\n    {{"name": "memory_query", "parameters": {{"query": "{query}"}}}}\n  ]\n}}\n```'}
            ]
        })
    
    # 4. 轻度担忧但不需要评估（仅 local_chat）
    mild_concern_samples = [
        "我有点担心明天的面试",
        "最近学习有点忙",
        "昨晚没睡好，今天有点困",
        "今天有点不开心",
        "面试前紧张怎么办",
        "有些事情想跟你说说",
        "最近有什么好看的书推荐吗",
        "如何处理同学之间的矛盾",
        "和室友相处有什么技巧吗",
    ]
    
    for text in mild_concern_samples:
        samples.append({
            "conversations": [
                {"from": "system", "value": NEW_SYSTEM_PROMPT},
                {"from": "user", "value": text},
                {"from": "assistant", "value": f'```json\n{{\n  "tools": [\n    {{"name": "local_chat", "parameters": {{"user_input": "{text}"}}}}\n  ]\n}}\n```'}
            ]
        })
    
    return samples


def create_sample_from_psychology(item: Dict) -> Dict:
    """从心理咨询数据创建样本"""
    user_input = item.get("input", "").strip()
    if not user_input or len(user_input) < 10:
        return None
    
    lines = user_input.split('\n')
    student_lines = [l.replace("学生: ", "").replace("学生：", "").strip() 
                     for l in lines if l.startswith("学生")]
    
    if not student_lines:
        return None
    
    first_input = student_lines[0]
    if len(first_input) > 100:
        first_input = first_input[:100]
    
    # 判断是否需要评估
    assessment_kw = ["自杀", "死", "活着没意思", "痛苦", "焦虑", "抑郁", "失眠", 
                     "睡不着", "压力大", "崩溃", "绝望", "害怕", "恐惧", "心慌",
                     "想死", "不想活", "轻生"]
    
    needs_assessment = any(kw in first_input for kw in assessment_kw)
    
    tools = [{"name": "local_chat", "parameters": {"user_input": first_input}}]
    
    if needs_assessment:
        trigger = "用户表达心理困扰"
        if any(kw in first_input for kw in ["自杀", "死", "想死", "轻生", "活着没意思"]):
            trigger = "用户表达自杀意念或生活无意义感"
        elif "焦虑" in first_input or "心慌" in first_input:
            trigger = "用户表达焦虑症状"
        elif "失眠" in first_input or "睡不着" in first_input:
            trigger = "用户表达严重睡眠问题"
        elif "抑郁" in first_input or "低落" in first_input:
            trigger = "用户表达抑郁情绪"
        elif "压力" in first_input:
            trigger = "用户表达压力和心理负担"
        
        tools.append({"name": "psychological_assessment", "parameters": {"trigger_reason": trigger}})
    
    output = {"tools": tools}
    
    return {
        "conversations": [
            {"from": "system", "value": NEW_SYSTEM_PROMPT},
            {"from": "user", "value": first_input},
            {"from": "assistant", "value": f"```json\n{json.dumps(output, ensure_ascii=False, indent=2)}\n```"}
        ]
    }


def create_sample_from_alpaca(item: Dict) -> Dict:
    """从 Alpaca 数据创建样本（仅 local_chat）"""
    instruction = item.get("instruction", "").strip()
    input_text = item.get("input", "").strip()
    
    if not instruction:
        return None
    
    user_input = f"{instruction}\n{input_text}".strip() if input_text else instruction
    
    if len(user_input) > 100:
        user_input = user_input[:100]
    
    tools = [{"name": "local_chat", "parameters": {"user_input": user_input}}]
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
    print("🔄 重构 Brain LLM 训练数据 V2")
    print("=" * 60)
    
    # 1. 创建核心样本
    print("\n1️⃣ 创建核心样本...")
    core_samples = create_core_samples()
    print(f"   ✅ 创建: {len(core_samples)} 条")
    
    # 2. 从心理咨询数据提取
    print("\n2️⃣ 从心理咨询数据提取...")
    with open("LLaMA-Factory/data/psychology_pending.json", "r", encoding="utf-8") as f:
        psych_data = json.load(f)
    
    psych_samples = []
    for item in psych_data:
        sample = create_sample_from_psychology(item)
        if sample:
            psych_samples.append(sample)
    
    # 限制数量
    psych_samples = psych_samples[:80]
    print(f"   ✅ 提取: {len(psych_samples)} 条")
    
    # 3. 从 Alpaca 数据提取
    print("\n3️⃣ 从 Alpaca 数据提取...")
    with open("LLaMA-Factory/data/alpaca_zh_demo.json", "r", encoding="utf-8") as f:
        alpaca_data = json.load(f)
    
    sampled_alpaca = random.sample(alpaca_data, min(120, len(alpaca_data)))
    
    alpaca_samples = []
    for item in sampled_alpaca:
        sample = create_sample_from_alpaca(item)
        if sample:
            alpaca_samples.append(sample)
    
    print(f"   ✅ 提取: {len(alpaca_samples)} 条")
    
    # 4. 合并
    print("\n4️⃣ 合并数据...")
    all_samples = core_samples + psych_samples + alpaca_samples
    random.shuffle(all_samples)
    
    print(f"   总计: {len(all_samples)} 条")
    
    # 5. 统计
    local_only = 0
    with_assessment = 0
    with_memory = 0
    
    for sample in all_samples:
        for conv in sample["conversations"]:
            if conv["from"] == "assistant":
                content = conv["value"]
                has_assessment = "psychological_assessment" in content
                has_memory = "memory_query" in content
                
                if has_assessment:
                    with_assessment += 1
                elif has_memory:
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

