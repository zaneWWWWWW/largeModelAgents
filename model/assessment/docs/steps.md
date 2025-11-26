日期: 2025-11-12

目标: 归一化并清洗 PsyQA、CPsyCounE、CPsyCounD 为统一 JSONL

标签体系: 使用四项标签体系（严格按键名与取值范围）：
- depression_level: 0–3（0 无明显抑郁；1 轻度；2 中度；3 重度）
- anxiety_level: 0–3（0 无明显焦虑；1 轻度；2 中度；3 重度）
- risk_flag: none | suicidal | self_harm | violence（如无风险输出 none）
- student_distress_score: 0–9（综合主观痛苦与学习/睡眠/人际影响的简分）

实现步骤:
- 新增脚本 `scripts/normalize_clean.py`，统一输出结构为:
  - 字段: `id`, `source`, `topic`, `conversation`, `meta`
  - 对话项: `{role: user|assistant, text: string}`
  - 清洗: 角色前缀剥离、空白规范化、电话/邮箱/社交号匿名化、短促无效应答过滤
- CPsyCounD 处理: 按行解析 `.txt`，识别角色并清洗，组装多轮
- CPsyCounE 处理: 解析主题子目录 JSON 数组，识别角色并清洗，保留 `topic`
- PsyQA 处理: 合并 `title + content` 作为用户问句；按点赞与长度选择最优答案，过滤噪声，形成单轮问答
- 统计: 生成 `data/processed/stats.json` 包含总量、来源分布、平均轮次与主题直方
- 输出: `data/processed/unified.jsonl` 每行一个样本

资源申请与执行:
- 参考 `scripts/python.sh`，调整为 SLURM A40 分区:
  - `#SBATCH --partition=A40 --gres=gpu:1 --cpus-per-task=8 --mem=32G --time=01:00:00`
  - 工作目录为 `/share/home/gpu093197/zanewang/project-llm`
  - 运行 `python scripts/normalize_clean.py`
- 修复换行符为 UNIX，提交作业 `sbatch scripts/python.sh`

运行结果:
- 作业提交: `Submitted batch job 49306`
- 生成文件:
  - `data/processed/unified.jsonl`
  - `data/processed/stats.json`
- 统计摘要:
  - `total`: 79423
  - `by_source`: CPsyCounD=3084, CPsyCounE=45, PsyQA=76294
  - `avg_turns`: CPsyCounD≈15.72, CPsyCounE≈13.13, PsyQA=2.0

后续计划:
- 在清洗后样本上运行 LLM 标注管线，生成结构化标签 JSON 并合并到训练集
- 对高风险与困难样本做人工抽检与提示迭代
日期: 2025-11-14

目标: 面向学生场景的进一步清洗与精简，降低 LLM 标注 token 成本

策略:
- 主题聚焦: 仅保留含学生/校园/学习/考试/舍友等线索的样本；对 PsyQA 同时基于标签过滤
- 质量阈值: 助手答复长度阈调到 ≥80 字；剔除低质量短答与噪声样本
- 上下文裁剪: 多轮对话仅保留尾部近 2000 字或最多 12 轮，保证近因与状态可评估
- 输出瘦身: 生成 `unified_min.jsonl` 与 `stats_min.json`

修改:
- 脚本: `scripts/normalize_clean.py`
  - 新增 `STUDENT_KEYS/MENTAL_KEYS` 与过滤函数
  - 提升助手答复最小长度至 80
  - 主题/标签双条件过滤 PsyQA
  - 裁剪多轮对话尾部（2000 字、≤12 轮）
  - 输出文件名改为 `unified_min.jsonl`，统计为 `stats_min.json`
- 资源任务: `scripts/python.sh` 更新作业名与日志前缀为 `normalize_min`

运行:
- 提交作业: `Submitted batch job 49507`
- 结果文件:
  - `data/processed/unified_min.jsonl`
  - `data/processed/stats_min.json`
- 统计摘要:
  - `total`: 19250（由 7.9 万缩至约 1.9 万）
  - `by_source`: CPsyCounD=1273, CPsyCounE=9, PsyQA=17968
  - `avg_turns`: CPsyCounD≈8.95, CPsyCounE≈9.56, PsyQA=2.0

下一步:
- 标签体系最终精简为 4 项：`depression_level`,`anxiety_level`,`risk_flag`,`student_distress_score`
  - `depression_level` 0–3
  - `anxiety_level` 0–3
  - `risk_flag` none | suicidal | self_harm | violence
  - `student_distress_score` 0–9（综合情绪/功能简分）
- 仅输出整数/枚举，禁用冗长描述；建议双阶段标注（证据→标签）用摘要降低输入 token
日期: 2025-11-15

目标: 为本地标注创建可复制目录，并将 `unified_min.jsonl` 拆分为每 300 条一份

操作:
- 新增目录 `labeling/`，包含数据、脚本、提示与说明
- 拆分脚本 `labeling/scripts/split_jsonl.py`，将 `data/processed/unified_min.jsonl` 分片到 `labeling/data/input_part_*.jsonl`
- 结果: 生成 65 份分片文件（每份约 300 条）
- 提供者支持: 增加 `Gemini` 提供者，环境变量 `PROVIDER=gemini`，`MODEL=gemini-2.5-flash`，`GEMINI_API_KEY=...`

运行说明:
- 安装依赖：`pip install -r labeling/requirements.txt`
- 执行标注：`python labeling/scripts/run_labeling.py --input labeling/data/input_part_0001.jsonl --output labeling/results/labels_0001.jsonl`

日期: 2025-11-16

目标: 完成本地 LLM 数据标注并归档可复现包

数据来源与分片:
- 原始清洗集: `data/processed/unified_min.jsonl`（约 1.9 万样本）
- 分片拆分: `labeling/data/input_part_0001.jsonl` 至 `input_part_0065.jsonl`（每份 300 条）

标注提示与提供者:
- 提示词: `labeling/repro_package/prompts/min_instruction.txt`（学生场景背景，四标签最小输出）
- 提供者: Gemini 2.5 Flash（`PROVIDER=gemini`，`MODEL=gemini-2.5-flash`，`GEMINI_API_KEY`）
- 运行脚本: `labeling/repro_package/src/run_labeling.py`

运行命令示例:
- 单分片: `python labeling/repro_package/src/run_labeling.py --input labeling/repro_package/data/input_part_0001.jsonl --output labeling/repro_package/results/input_part_0001.jsonl`
- 批量执行: 逐分片运行并生成 `results/input_part_*.jsonl`

结果归档:
- 合并结果: `labeling/repro_package/results/dataset.jsonl`（包含四标签：`depression_level`,`anxiety_level`,`risk_flag`,`student_distress_score`）
- 日志与复现: `labeling/repro_package/results/labeling.log`，`labeling/repro_package/README.md`

训练数据集准备:
- 生成指令微调集: `python scripts/prepare_sft_dataset.py`
- 输出: `data/datasets/sft_student_mental.jsonl`（`instruction`/`input`/`output` 三字段；`output` 为四标签 JSON 字符串）

日期: 2025-11-19

目标: 使用 LLaMA-Factory 对 DeepSeek-R1-Distill-Qwen-1.5B 进行 LoRA 微调，并修复下载、模板与数据问题

关键修改与修复:
- 镜像与本地缓存: 通过镜像预下载模型到本地，训练指向本地路径，避免 SSL 证书问题与作业节点外网访问
  - 模型路径: `/share/home/gpu093197/.cache/huggingface/hub/deepseek-r1-qwen1p5b`
  - 脚本: `scripts/train_llf_r1_qwen1p5b.sh` 设置 `HF_HOME` 与 `PYTHONPATH`
- 模板修复: 将训练配置中的 `template` 更正为存在的 `qwen`
  - 配置: `llamafactory/configs/train_r1_qwen1p5b_lora.yaml` 中 `template: qwen`
- 数据列映射修复: 使 `alpaca` 转换器正确读取三列并产生有效样本
  - 修改: `data/datasets/dataset_info.json` 将 `columns` 映射为 `prompt: instruction`, `query: input`, `response: output`
- 训练集空集问题: 删除 `max_samples: 0`，避免将数据集截断为空
  - 配置: `llamafactory/configs/train_r1_qwen1p5b_lora.yaml`
- 4bit 量化依赖: 安装并验证 `bitsandbytes>=0.39.0`（实际版本 `0.43.3`），解决量化初始化检查失败
  - 环境: `conda activate llama-zanewang` 后安装 `bitsandbytes`，验证可导入
- 数值稳定性: 增加 `upcast_layernorm: true` 与 `upcast_lmhead_output: true` 提升 4bit 训练稳定性
  - 配置: `llamafactory/configs/train_r1_qwen1p5b_lora.yaml`

训练配置（摘要）:
- 模型: 本地缓存 `deepseek-r1-qwen1p5b`
- 阶段: `sft`
- LoRA: `rank=16, alpha=32, dropout=0.05, target=all`
- 量化: `quantization_bit: 4`
- 训练: `per_device_train_batch_size: 8`, `gradient_accumulation_steps: 4`, `learning_rate: 1e-4`, `num_train_epochs: 3`, `bf16: true`
- 评估: `val_size: 0.05`, `eval_strategy: steps`, `eval_steps: 1000`

作业记录:
- 49824: FAILED（模板名错误 `qwen2`，更正为 `qwen`）
- 49868: 运行中，后续因 `max_samples: 0` 导致空集 `StopIteration`，已删除该参数
- 49875: 再次运行，进入数据与分词流程后，报 `Cannot find valid samples`，随后修复数据列映射
- 49891: 触发 `bitsandbytes` 缺失错误，安装后修复
- 49906: COMPLETED，用时 `05:49:14`，节点 `comput07`

训练产物:
- 目录: `saves/r1-qwen1p5b/lora/sft`
- 最新输出: `adapter_model.safetensors`, `adapter_config.json`, `train_results.json`, `eval_results.json`, `training_loss.png`, `training_eval_loss.png`
- 检查点: `checkpoint-1000/`, `checkpoint-1716/`（均含 `adapter_model.safetensors` 等文件）

后续步骤:
- 合并 LoRA 权重到基础模型，生成推理模型目录
- 运行快速验证推理，使用 `qwen` 模板检查输出格式
- 导出 GGUF 并量化（如 `Q4_K_M`）以适配移动端部署
