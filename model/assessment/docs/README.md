# 项目概览：学生心理评估大模型微调与部署

本项目旨在基于本地清洗的心理咨询数据，对多种基础模型进行 LoRA 微调，产出可用于学生场景心理状态快速评估的模型，并支持后续在移动端量化部署（GGUF）。当前已完成基于 DeepSeek-R1-Distill-Qwen-1.5B 的一次训练，后续将扩展到更多模型（如 Qwen3 系列、LLaMA、Gemma、Mistral 等）。

## 背景与目标
- 背景：数据来自单轮与多轮心理咨询语料，围绕学生群体的学习、考试、人际、情绪困扰等主题，已完成统一清洗与精简。
- 目标：微调模型以输出四项标签，满足轻量化部署与实时评估需求。
- 标签体系（严格按键名与取值范围）：
  - `depression_level`: 0–3
  - `anxiety_level`: 0–3
  - `risk_flag`: `none | suicidal | self_harm | violence`
  - `student_distress_score`: 0–9

## 目录结构（关键路径）
- `data/`
  - `rawdata/` 原始数据集（CPsyCounD/E、PsyQA 等）
  - `processed/` 清洗与精简输出：
    - `unified.jsonl`、`stats.json`
    - `unified_min.jsonl`、`stats_min.json`
  - `datasets/` 训练输入：
    - `sft_student_mental.jsonl`（三列：`instruction/input/output`）
    - `dataset_info.json`（映射为 `prompt/query/response`）
- `labeling/` 本地标注与复现包
  - `prompts/min_instruction.txt`（四项标签提示）
  - `scripts/` 运行与拆分脚本
  - `repro_package/` 标注复现（数据/代码/日志）
- `llamafactory/` 项目本地 LLaMA-Factory 配置
  - `configs/train_r1_qwen1p5b_lora.yaml`（训练配置）
- `LLaMA-Factory-main/` LLaMA-Factory 源码（`src/llamafactory/...`）
- `scripts/`
  - `train_llf_r1_qwen1p5b.sh`（SLURM 提交脚本）
- `saves/r1-qwen1p5b/lora/sft/` 训练产物目录
  - `adapter_model.safetensors`、`adapter_config.json`、`train_results.json`、`eval_results.json`、`training_loss.png` 等
  - `checkpoint-1000/`、`checkpoint-1716/`（中间检查点）
- `steps/steps.md` 项目过程记录与变更说明

## 数据处理流水线
1. 清洗与归一化（`scripts/normalize_clean.py`）
   - 统一结构，剥离角色前缀、匿名化、过滤短促无效应答
   - 统计信息生成：`data/processed/stats.json`
2. 学生场景精简（`unified_min.jsonl`）
   - 主题聚焦与质量阈值（助手答复 ≥80 字）
   - 多轮对话裁剪（尾部近因，2000 字或 ≤12 轮）
3. 标注复现包（`labeling/repro_package/`）
   - 使用 `min_instruction.txt` 提示词产出四项标签，合并为 `results/dataset.jsonl`
4. SFT 数据集准备（`scripts/prepare_sft_dataset.py`）
   - 输出 `data/datasets/sft_student_mental.jsonl`，三列：`instruction/input/output`，`output` 为四标签 JSON 字符串

## 训练与作业
- 配置约定：每个模型有独立的训练 YAML 与提交脚本，命名规范示例：
  - 配置：`llamafactory/configs/train_<model_short_name>_<method>.yaml`（如 `train_r1_qwen1p5b_lora.yaml`）
  - 脚本：`scripts/train_llf_<model_short_name>.sh`（如 `train_llf_r1_qwen1p5b.sh`）
- 通用参数（可复用到不同模型）：
  - 阶段：`sft`
  - 模板：与模型匹配（如 `qwen`、`qwen3`、`llama` 等）
  - LoRA：`rank=16, alpha=32, dropout=0.05, target=all`
  - 量化：`quantization_bit: 4`（需安装 `bitsandbytes`，建议开启 `upcast_layernorm/lmhead`）
  - 训练：`batch_size=8`，`gradient_accumulation_steps=4`，`lr=1e-4`，`epochs=3`，`bf16=true`
  - 评估：`val_size=0.05`，`eval_strategy=steps`，`eval_steps=1000`
- 环境：`conda activate llama-zanewang`，设置 `HF_HOME` 与 `PYTHONPATH`
- 当前已完成的作业：
  - `49906`（DeepSeek-R1-Qwen1.5B）：`COMPLETED`，`Elapsed=05:49:14`（产物在 `saves/r1-qwen1p5b/lora/sft/`）

## 常见问题与修复要点
- 下载与证书：优先镜像预下载到本地缓存，训练指向本地路径，避免 SSL 错误
- 模板名：使用 `qwen`（`qwen2` 不存在）
- 数据映射：`dataset_info.json` 中将 `instruction/input/output` 映射为 `prompt/query/response`
- 空集问题：避免 `max_samples: 0`（会导致 `StopIteration`）
- 4bit 量化依赖：安装 `bitsandbytes>=0.39.0`（当前为 `0.43.3`），并开启 `upcast_*` 提升稳定性

## 产物与评估
- 训练产物：`saves/r1-qwen1p5b/lora/sft/adapter_model.safetensors` 与评估日志图
- 快速评估：查看 `eval_results.json` 与损失曲线（`training_loss.png` / `training_eval_loss.png`）

## 下一步（可选）
- 合并 LoRA 权重到基础模型，生成可推理的完整模型目录
- 导出 GGUF 并量化（如 `Q4_K_M`）用于移动端部署
- 运行推理验证：使用 `qwen` 模板进行多轮输入，确认输出 JSON 格式与取值范围正确

## 环境与资源
- 计算：SLURM A40 分区（`--gres=gpu:1`，`--cpus-per-task=8`，`--mem=32G`）
- 缓存：`HF_HOME=/share/home/gpu093197/.cache/huggingface`
- 关键依赖：`transformers`、`datasets`、`peft`、`bitsandbytes`、`accelerate`、`wandb`（如启用）

## 源码与路径说明
- 目录 `LLaMA-Factory` 若为空可忽略，项目实际使用的是本仓库中已引入的源码目录：`LLaMA-Factory-main/`。
- 训练脚本通过设置 `PYTHONPATH="LLaMA-Factory-main/src"` 来引用本地源码；迁移到新环境时有两种方式：
  - 保留并使用本地 `LLaMA-Factory-main/`（推荐以子模块或 vendor 方式管理）
  - 或改为通过 `pip install LLaMA-Factory` 安装官方发行版，但需注意与本地改动的差异
- 如需重新拉取官方源码，可在项目根目录克隆到 `LLaMA-Factory-main/` 并保持脚本中的 `PYTHONPATH` 指向其 `src`。

## 重要路径索引
- 训练配置（示例与约定）：`llamafactory/configs/train_<model_short_name>_<method>.yaml`
- 训练脚本（示例与约定）：`scripts/train_llf_<model_short_name>.sh`
- 数据映射：`data/datasets/dataset_info.json`
- 训练产物（分模型归档）：`saves/<model_short_name>/<method>/sft/`
- 过程记录：`steps/steps.md`

## 新模型接入工作流
- 预下载模型到本地缓存（建议使用镜像），并确认 `model_name_or_path` 指向本地路径
- 创建训练配置：复制现有 YAML，调整模板（如 `qwen`/`qwen3`/`llama`）、上下文长度与训练参数
- 检查数据映射：`dataset_info.json` 保持 `prompt/query/response` 对应 `instruction/input/output`
- 提交作业：新增 `scripts/train_llf_<model_short_name>.sh` 并提交到 SLURM A40 分区
- 验证与归档：监控 `squeue/sacct`，产物保存到 `saves/<model_short_name>/<method>/sft/`
- 可选：合并 LoRA、导出 GGUF（`Q4_K_M` 等）并进行推理验证
## 联系与复现
- 迁移后，为 AI 助手提供：本 README、`steps/steps.md`、训练配置与数据映射文件即可快速理解并复现训练流程。