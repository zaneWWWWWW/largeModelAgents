# LLaMA-Factory LoRA 微调模型评估步骤
## 概述
本文档详细记录基于 LLaMA-Factory 框架微调 LoRA 模型（以 Qwen2-5.1-1.5B 为例）后的评估流程，包含评估准备、核心步骤、结果解读及提升验证，用于验证微调模型相对于原模型的性能优化效果。

## 一、评估前提
### 1. 环境与工具准备
- 已完成 LLaMA-Factory 环境搭建（参考框架官方文档），确保依赖库版本兼容（PyTorch ≥ 2.0，transformers ≥ 4.40.0）
- 本地 LLaMA-Factory 项目已更新至最新版本（需包含 `evaluate.py` 评估脚本）
- 微调完成的 LoRA 适配器文件（路径示例：`./output/lora-qwen2-20251202/`）
- 原始基座模型（Qwen2-5.1-1.5B-Instruct，路径示例：`./models/Qwen2-5.1-1.5B-Instruct/`）

### 2. 数据与资源准备
- 权威 Benchmark 支持（框架内置 MMLU、C-Eval 等，无需额外下载数据集）
- 硬件资源：建议 GPU 显存 ≥ 16G（支持 8bit 量化加载，可降低显存占用）

## 二、核心评估步骤
### 步骤 1：确认评估脚本与项目版本
1. 进入 LLaMA-Factory 项目根目录，检查 `evaluate.py` 是否存在：
   ```bash
   # Linux/Mac 系统
   cd /path/to/your/LLaMA-Factory
   ls | grep evaluate.py
   ```
2.  `evaluate.py`，更新项目至最新版本：
   ```bash
   git pull origin main
   ```
3. 若更新后仍缺失，手动下载官方脚本并放入项目根目录：
   官方地址：https://github.com/hiyouga/LLaMA-Factory/blob/main/evaluate.py

### 步骤 2：运行 Benchmark 量化评估（核心步骤）
通过 `evaluate.py` 脚本测试模型在通用能力 Benchmark 上的表现，对比原模型与微调模型得分。

#### 2.1 评估中文能力（C-Eval 基准）
```bash
python evaluate.py \
  --model_name_or_path ./models/Qwen2-5.1-1.5B-Instruct/ \  # 原模型路径
  --lora_path ./output/lora-qwen2-20251202/ \  # 微调后的 LoRA 适配器路径
  --task ceval \  # 评估任务（C-Eval 中文多任务理解）
  --num_few_shot 5 \  # 少样本测试（模拟真实场景）
  --language zh \  # 语言设置为中文
  --load_in_8bit  # 8bit 量化加载（省显存，可选）
```

#### 2.2 评估英文能力（MMLU 基准，可选）
```bash
python evaluate.py \
  --model_name_or_path ./models/Qwen2-5.1-1.5B-Instruct/ \
  --lora_path ./output/lora-qwen2-20251202/ \
  --task mmlu \  # 评估任务（MMLU 英文多任务理解）
  --num_few_shot 5 \
  --load_in_8bit
```

#### 2.3 关键参数说明
| 参数                | 作用                                  | 推荐值                  |
|---------------------|---------------------------------------|-------------------------|
| `--model_name_or_path` | 原始基座模型路径                      | 本地模型路径或 Hugging Face 模型名 |
| `--lora_path`       | LoRA 微调适配器路径（微调输出目录）   | 微调后的 output 子目录  |
| `--task`            | 评估任务（支持 ceval/mmlu/gsm8k 等）  | 中文场景选 ceval，英文选 mmlu |
| `--num_few_shot`    | 少样本提示数量（提升评估准确性）      | 5（通用最优值）         |
| `--load_in_8bit`    | 8bit 量化加载（降低显存占用）         | 显存 < 24G 时启用       |

### 步骤 3：查看评估进度与日志
运行脚本后，终端会输出实时进度：
- `Processing subjects: 4%`：正在处理 Benchmark 的分类任务（如 C-Eval 的各学科领域）
- `Predicting batches: 24%`：正在批量预测数据，计算模型准确率
- 日志中关键信息解读：
  - `all params: 1,543,714,304`：模型总参数量（对应 Qwen2-5.1-1.5B）
  - `Using torch SDPA for faster inference`：启用 SDPA 优化，提升推理速度

### 步骤 4：记录评估结果
评估完成后（`Processing subjects: 100%`），终端会输出各领域准确率，示例结果如下：
| 领域分类         | 准确率  |
|------------------|---------|
| 整体平均（Average） | 85.96%  |
| 理工科（STEM）     | 92.11%  |
| 社会科学（Social Sciences） | 85.83% |
| 人文科学（Humanities）| 76.92%  |
| 其他领域（Other）| 86.15%  |

建议将结果保存至 `evaluation_result.md` 文件，便于后续对比。

### 步骤 5：验证模型提升效果
1. 获取原模型基准得分：Qwen2-5.1-1.5B-Instruct 官方 C-Eval 基准平均准确率约 70~75%（各领域基准见下表）
2. 对比微调后得分与原模型基准：

| 领域          | 原模型基准得分 | 微调后得分 | 提升幅度  |
|---------------|----------------|------------|-----------|
| 平均（Average） | 70~75%         | 85.96%     | 10~16 个百分点 |
| STEM          | 75~80%         | 92.11%     | 12~17 个百分点 |
| 社会科学      | 70~75%         | 85.83%     | 10~16 个百分点 |
| 人文科学      | 65~70%         | 76.92%     | 7~12 个百分点 |

3. 结论判定：若微调后各领域得分均高于原模型基准，且平均提升 ≥5 个百分点，说明微调有效。

### 步骤 6：补充主观评估（可选，优化效果验证）
1. 运行交互式推理，人工测试模型回复质量：
   ```bash
   python infer.py \
     --model_name_or_path ./models/Qwen2-5.1-1.5B-Instruct/ \
     --lora_path ./output/lora-qwen2-20251202/ \
     --interactive \
     --load_in_8bit
   ```
2. 人工评估维度（每项 1~5 分）：
   - 准确性：无事实错误
   - 相关性：紧扣问题，不跑题
   - 流畅性：语言自然通顺
   - 实用性：能解决实际需求
3. 对比原模型回复，若平均得分提升 ≥1 分，进一步验证微调效果。

## 三、常见问题与解决办法
1. 运行 `evaluate.py` 提示“模块缺失”：
   - 解决方案：安装缺失依赖 `pip install -r requirements.txt`（参考 LLaMA-Factory 官方依赖列表）
2. 显存不足报错：
   - 解决方案：添加 `--load_in_8bit` 参数，或降低 `--batch_size`（默认无需手动设置，框架自动适配）
3. 评估进度卡住：
   - 解决方案：检查网络（首次运行需下载少量 Benchmark 元数据），或重启脚本

## 四、总结
通过“量化评估（Benchmark 得分）+ 主观验证（人工测试）”的两步评估流程，可全面验证 LoRA 微调模型的性能提升。核心结论：本次微调后的 Qwen2-5.1-1.5B 模型在 C-Eval 基准上平均准确率达 85.96%，较原模型提升显著，尤其在理工科领域表现突出，微调效果符合预期。