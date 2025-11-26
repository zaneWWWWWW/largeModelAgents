import json, os
from transformers import AutoTokenizer

in_files = [
    '/root/autodl-tmp/Qwen/LLaMA-Factory/data/train_data_multi_50pct.jsonl',
    '/root/autodl-tmp/Qwen/LLaMA-Factory/data/merged_30pct.jsonl',
]
out_file = '/root/autodl-tmp/Qwen/LLaMA-Factory/data/clean_merged.jsonl'
tokenizer = AutoTokenizer.from_pretrained('/root/autodl-tmp/Qwen/Qwen2.5-1.5B-Instruct')

seen = set()
max_tokens = 2048
with open(out_file, 'w', encoding='utf-8') as w:
    for f in in_files:
        with open(f, 'r', encoding='utf-8') as r:
            for line in r:
                obj = json.loads(line)
                msgs = obj.get('messages', [])
                if not msgs: continue
                user = ''.join([m['content'] for m in msgs if m['role']=='user'])
                assistant = ''.join([m['content'] for m in msgs if m['role']=='assistant'])
                key = (user.strip(), assistant.strip())
                if key in seen: continue
                text = user + '\n' + assistant
                if len(tokenizer(text)['input_ids']) > max_tokens: continue
                seen.add(key)
                w.write(json.dumps({'messages': msgs}, ensure_ascii=False) + '\n')
print('saved:', out_file)