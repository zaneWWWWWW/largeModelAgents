import json
import glob
import os

# 读取提示词
with open('data/min_instruction.txt', 'r', encoding='utf-8') as f:
    instruction_text = f.read().strip()

output_data = []
input_files = sorted(glob.glob('data/input_part_*.jsonl'))

print(f"找到 {len(input_files)} 个数据文件，开始处理...")

for file_path in input_files:
    with open(file_path, 'r', encoding='utf-8') as f:
        for line in f:
            try:
                item = json.loads(line)
                
                # 1. 构建 Input (拼接对话历史)
                conversation_text = ""
                for msg in item.get('conversation', []):
                    role = "用户" if msg['role'] == 'user' else "咨询师"
                    conversation_text += f"{role}: {msg['text']}\n"
                
                # 2. 构建 Output (JSON 格式的评估结果)
                output_obj = {
                    "depression_level": item.get('depression_level', 0),
                    "anxiety_level": item.get('anxiety_level', 0),
                    "risk_flag": item.get('risk_flag', 'none'),
                    "student_distress_score": item.get('student_distress_score', 0)
                }
                # 确保输出是紧凑的 JSON 字符串，符合 prompt 要求
                output_json = json.dumps(output_obj, ensure_ascii=False)
                
                # 3. 组合成 Alpaca 格式
                alpaca_entry = {
                    "instruction": instruction_text,
                    "input": conversation_text.strip(),
                    "output": output_json
                }
                output_data.append(alpaca_entry)
                
            except json.JSONDecodeError:
                print(f"Skipping invalid line in {file_path}")
                continue

# 保存合并后的数据集
output_file = 'LLaMA-Factory/data/psychology_train.json'
with open(output_file, 'w', encoding='utf-8') as f:
    json.dump(output_data, f, indent=2, ensure_ascii=False)

print(f"处理完成！共生成 {len(output_data)} 条训练数据。")
print(f"保存至: {output_file}")









