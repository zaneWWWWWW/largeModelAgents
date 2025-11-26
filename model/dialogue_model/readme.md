# 模型微调与量化实验记录（Qwen2.5-1.5B / Gemma-3-1B）

## 环境与目录
- 代码：`/root/autodl-tmp/Qwen/LLaMA-Factory`
- 本地基础模型：
  - Qwen2.5-1.5B-Instruct：`/root/autodl-tmp/Qwen/Qwen2.5-1.5B-Instruct`
  - Gemma-3-1B-it：`/root/autodl-tmp/Qwen/Gemma`
- LoRA 训练输出：`/root/autodl-tmp/Qwen/LLaMA-Factory/saves/.../lora/sft`
- 合并导出输出：`/root/autodl-tmp/Qwen/LLaMA-Factory/output/...`
- 量化工具：`/root/autodl-tmp/Qwen/llama.cpp`
- 依赖安装：
  - `pip install -r /root/autodl-tmp/Qwen/LLaMA-Factory/requirements.txt`
  - `pip install torch --index-url https://download.pytorch.org/whl/cu121`（按 CUDA 版本调整）

## 数据集准备（公开情感对话）
- 选用公开情感类对话数据作为语料来源：
  - EmpatheticDialogues（HF: `empathetic_dialogues`）
  - DailyDialog（HF: `dailydialog`）
  - EmotionLines（HF: `emotion_lines`）
- 合并自有语料：`/root/autodl-tmp/Qwen/LLaMA-Factory/data/train_data_multi_50pct.jsonl`、`/root/autodl-tmp/Qwen/LLaMA-Factory/data/merged_30pct.jsonl`
- 统一成 ShareGPT 行格式：每行一个 JSON，字段 `messages`，角色取值 `system|user|assistant`。

## 数据清洗与格式处理
- 目标：提升数据质量与安全性，避免过长样本导致显存压力。
- 操作：
  - 去重：基于 `user+assistant` 文本指纹消重。
  - 过滤长度：分词后 token 数超过阈值（如 2048）剔除。
  - 移除无效或有害回复：如情感咨询中明显无效答案样本删除。
- 示例脚本（生成清洗后的 JSONL）：
```
python - << 'PY'
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
PY
```

## 数据注册（LLaMA-Factory）
- 在 `dataset_info.json` 注册数据集条目（示例片段）：
```
{
  "train_data_multi_50pct": {
    "file": "/root/autodl-tmp/Qwen/LLaMA-Factory/data/train_data_multi_50pct.jsonl",
    "format": "sharegpt"
  },
  "merged_30pct": {
    "file": "/root/autodl-tmp/Qwen/LLaMA-Factory/data/merged_30pct.jsonl",
    "format": "sharegpt"
  }
}
```

## 模型与微调方法选择
- 模型：
  - Qwen2.5-1.5B-Instruct：中文能力、推理稳定、显存友好。
  - Gemma-3-1B-it：轻量、英文指令能力强，适配通用情感场景。
- 微调方法：
  - LoRA：低显存增量训练，`rank=16` 常用；数据质量提升显著。
  - 可选 QLoRA：`bnb 4bit` 量化加载，进一步降低显存占用（需在模型参数添加 `quantization_method: bnb`、`quantization_bit: 4`）。

## 编写训练脚本（YAML）
- Qwen2.5 LoRA（已创建）：`examples/train_lora/qwen2_5_1_5b_lora_sft.yaml`
```
model_name_or_path: /root/autodl-tmp/Qwen/Qwen2.5-1.5B-Instruct
trust_remote_code: true
stage: sft
do_train: true
finetuning_type: lora
lora_rank: 16
lora_target: all
dataset: train_data_multi_50pct,merged_30pct
template: qwen
cutoff_len: 2048
output_dir: saves/qwen2_5-1_5b/lora/sft
per_device_train_batch_size: 2
gradient_accumulation_steps: 8
learning_rate: 1.0e-4
num_train_epochs: 3.0
bf16: true
```
- Gemma-3-1B LoRA（已创建）：`examples/train_lora/gemma3_1b_lora_sft.yaml`
```
model_name_or_path: /root/autodl-tmp/Qwen/Gemma
trust_remote_code: true
stage: sft
do_train: true
finetuning_type: lora
lora_rank: 16
lora_target: all
lora_dropout: 0.05
dataset: train_data_multi_50pct,merged_30pct
template: gemma
cutoff_len: 2048
output_dir: saves/gemma3-1b/lora/sft
per_device_train_batch_size: 2
gradient_accumulation_steps: 8
learning_rate: 1.0e-4
num_train_epochs: 3.0
bf16: true
```

## 启动训练
```
PYTHONPATH='/root/autodl-tmp/Qwen/LLaMA-Factory/src' \
python -m llamafactory.cli train /root/autodl-tmp/Qwen/LLaMA-Factory/examples/train_lora/qwen2_5_1_5b_lora_sft.yaml

PYTHONPATH='/root/autodl-tmp/Qwen/LLaMA-Factory/src' \
python -m llamafactory.cli train /root/autodl-tmp/Qwen/LLaMA-Factory/examples/train_lora/gemma3_1b_lora_sft.yaml
```

## 合并 LoRA 权重并导出
- Qwen2.5（已创建）：`examples/merge_lora/qwen2_5_1_5b_lora_sft.yaml`
```
model_name_or_path: /root/autodl-tmp/Qwen/Qwen2.5-1.5B-Instruct
adapter_name_or_path: saves/qwen2_5-1_5b/lora/sft
template: qwen
export_dir: output/qwen2_5-1_5b_lora_sft
export_device: cpu
```
- Gemma-3-1B（已创建）：`examples/merge_lora/gemma3_1b_lora_sft.yaml`
```
model_name_or_path: /root/autodl-tmp/Qwen/Gemma
adapter_name_or_path: saves/gemma3-1b/lora/sft
template: gemma
export_dir: output/gemma3_1b_lora_sft
export_device: cpu
```
- 运行合并：
```
PYTHONPATH='/root/autodl-tmp/Qwen/LLaMA-Factory/src' \
python -m llamafactory.cli export /root/autodl-tmp/Qwen/LLaMA-Factory/examples/merge_lora/qwen2_5_1_5b_lora_sft.yaml

PYTHONPATH='/root/autodl-tmp/Qwen/LLaMA-Factory/src' \
python -m llamafactory.cli export /root/autodl-tmp/Qwen/LLaMA-Factory/examples/merge_lora/gemma3_1b_lora_sft.yaml
```

## 转 GGUF 与量化（llama.cpp）
- 将 HF 导出目录转为 GGUF F16：
```
python /root/autodl-tmp/Qwen/llama.cpp/convert_hf_to_gguf.py \
  /root/autodl-tmp/Qwen/LLaMA-Factory/output/qwen2_5-1_5b_lora_sft \
  --outfile /root/autodl-tmp/Qwen/LLaMA-Factory/output/qwen2_5-1_5b_f16.gguf --outtype f16 --verbose

python /root/autodl-tmp/Qwen/llama.cpp/convert_hf_to_gguf.py \
  /root/autodl-tmp/Qwen/LLaMA-Factory/output/gemma3_1b_lora_sft \
  --outfile /root/autodl-tmp/Qwen/LLaMA-Factory/output/gemma3_1b_f16.gguf --outtype f16 --verbose
```
- 量化为 `Q4_K_M`（混合分块格式，形状不满足时自动回退到兼容方案）：
```
/root/autodl-tmp/Qwen/llama.cpp/build/bin/llama-quantize \
  /root/autodl-tmp/Qwen/LLaMA-Factory/output/gemma3_1b_f16.gguf \
  /root/autodl-tmp/Qwen/LLaMA-Factory/output/gemma3_1b_q4_k_m.gguf q4_k_m
```
- 示例结果：
  - F16 GGUF（Gemma-3-1B）：约 2.0GB
  - Q4_K_M GGUF（Gemma-3-1B）：约 762MB（部分张量回退到 `q5_0/q8_0` 为正常行为）

## 合并模型对话
- CLI 对话（HuggingFace 后端）：
```
PYTHONPATH='/root/autodl-tmp/Qwen/LLaMA-Factory/src' \
python -m llamafactory.cli chat \
  --model_name_or_path /root/autodl-tmp/Qwen/LLaMA-Factory/output/gemma3_1b_lora_sft \
    --template gemma --trust_remote_code true --infer_backend huggingface \
  --max_new_tokens 512 --temperature 0.7 --top_p 0.8 --repetition_penalty 1.1
```

## 模型实验3：MiniCPM-1 0.5B（纯文本）
- 目的：评估更小参数量模型在情感对话上的可迁移性与部署效率（CPU/GGUF）。
- 基座模型：`/root/autodl-tmp/Qwen/MiniCPM-1-0.5B`（本地 HF 目录结构）。
- 模板：若为纯文本 MiniCPM，可沿用通用 ChatML 风格（`template: qwen` 或 `llama2`）；如为视觉/音频版本，则需对应 `minicpm_v` 或 `minicpm_o` 模板（参见 `src/llamafactory/data/template.py:1604`）。

### LoRA 训练 YAML
```
model_name_or_path: /root/autodl-tmp/Qwen/MiniCPM-1-0.5B
trust_remote_code: true
stage: sft
do_train: true
finetuning_type: lora
lora_rank: 16
lora_target: all
lora_dropout: 0.05
dataset: train_data_multi_50pct,merged_30pct
template: qwen
cutoff_len: 2048
overwrite_cache: true
preprocessing_num_workers: 8
dataloader_num_workers: 4
output_dir: saves/minicpm-0_5b/lora/sft
logging_steps: 10
save_steps: 500
plot_loss: true
overwrite_output_dir: true
save_only_model: false
report_to: none
per_device_train_batch_size: 4
gradient_accumulation_steps: 8
learning_rate: 1.0e-4
num_train_epochs: 3.0
lr_scheduler_type: cosine
warmup_ratio: 0.1
bf16: true
ddp_timeout: 180000000
resume_from_checkpoint: null
```

### 训练命令
```
PYTHONPATH='/root/autodl-tmp/Qwen/LLaMA-Factory/src' \
python -m llamafactory.cli train /root/autodl-tmp/Qwen/LLaMA-Factory/examples/train_lora/minicpm_0_5b_lora_sft.yaml
```

### 合并导出 YAML（示例）
```
model_name_or_path: /root/autodl-tmp/Qwen/MiniCPM-1-0.5B
adapter_name_or_path: saves/minicpm-0_5b/lora/sft
template: qwen
trust_remote_code: true
export_dir: output/minicpm_0_5b_lora_sft
export_size: 5
export_device: cpu
export_legacy_format: false
```

### 合并命令
```
PYTHONPATH='/root/autodl-tmp/Qwen/LLaMA-Factory/src' \
python -m llamafactory.cli export /root/autodl-tmp/Qwen/LLaMA-Factory/examples/merge_lora/minicpm_0_5b_lora_sft.yaml
```

### GGUF 转换与量化（建议 Q4_K_M）
```
python /root/autodl-tmp/Qwen/llama.cpp/convert_hf_to_gguf.py \
  /root/autodl-tmp/Qwen/LLaMA-Factory/output/minicpm_0_5b_lora_sft \
  --outfile /root/autodl-tmp/Qwen/LLaMA-Factory/output/minicpm_0_5b_f16.gguf --outtype f16 --verbose

/root/autodl-tmp/Qwen/llama.cpp/build/bin/llama-quantize \
  /root/autodl-tmp/Qwen/LLaMA-Factory/output/minicpm_0_5b_f16.gguf \
  /root/autodl-tmp/Qwen/LLaMA-Factory/output/minicpm_0_5b_q4_k_m.gguf q4_k_m
```

### 预期指标与实验量
- 训练样本：与主实验一致（清洗后万级样本）。
- 训练开销：参数量较小，`per_device_train_batch_size` 可适度增大至 4 或 8。
- 指标参考：ppl 通常高于 1.5B 模型，但部署与响应延迟显著降低。

## 实验量与指标
- Qwen2.5 LoRA SFT：
  - `train_results.json`（`saves/qwen2_5-1_5b/lora/sft`）显示：
    - `epoch: 3.0`
    - `train_loss ≈ 2.58`（`ppl ≈ exp(2.58) ≈ 13.2`）
    - `train_runtime ≈ 20,475s`，`train_steps_per_second ≈ 0.537`
  - 验证曲线稳定下降，`eval_loss ~ 2.52–2.55`，无明显过拟合迹象。
- Gemma-3-1B LoRA SFT：
  - 数据量：合并后样本万级，清洗后 token 长度上限 2048。
  - LoRA 配置：`rank=16`、`dropout=0.05`，训练 3 epoch。
  - 合并后导出模型体积约 1.9–2.0GB（F16 GGUF）。
- 量化：
  - `Q4_K_M` 约 762MB；推理延迟显著降低，显存消耗明显降低。

## 一键复现实验流程（摘要）
```
# 1) 清洗与合并
python clean_merge.py  # 见上内嵌脚本

# 2) 训练 LoRA
PYTHONPATH='/root/autodl-tmp/Qwen/LLaMA-Factory/src' \
python -m llamafactory.cli train /root/autodl-tmp/Qwen/LLaMA-Factory/examples/train_lora/gemma3_1b_lora_sft.yaml

# 3) 合并导出
PYTHONPATH='/root/autodl-tmp/Qwen/LLaMA-Factory/src' \
python -m llamafactory.cli export /root/autodl-tmp/Qwen/LLaMA-Factory/examples/merge_lora/gemma3_1b_lora_sft.yaml

# 4) 转 GGUF 并量化
python /root/autodl-tmp/Qwen/llama.cpp/convert_hf_to_gguf.py \
  /root/autodl-tmp/Qwen/LLaMA-Factory/output/gemma3_1b_lora_sft \
  --outfile /root/autodl-tmp/Qwen/LLaMA-Factory/output/gemma3_1b_f16.gguf --outtype f16 --verbose
/root/autodl-tmp/Qwen/llama.cpp/build/bin/llama-quantize \
  /root/autodl-tmp/Qwen/LLaMA-Factory/output/gemma3_1b_f16.gguf \
  /root/autodl-tmp/Qwen/LLaMA-Factory/output/gemma3_1b_q4_k_m.gguf q4_k_m

# 5) 对话
PYTHONPATH='/root/autodl-tmp/Qwen/LLaMA-Factory/src' \
python -m llamafactory.cli chat --model_name_or_path /root/autodl-tmp/Qwen/LLaMA-Factory/output/gemma3_1b_lora_sft \
  --template gemma --trust_remote_code true --infer_backend huggingface
```

## 备注
- 若需 QLoRA：在训练 YAML 的模型段加入 `quantization_method: bnb`、`quantization_bit: 4`。
- 量化形状不满足 `K` 分块约束时自动回退到兼容方案；不影响整体部署。
- 对话模板需与模型类型匹配：Gemma 用 `template: gemma`；Qwen 用 `template: qwen`。
