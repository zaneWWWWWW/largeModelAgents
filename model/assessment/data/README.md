# 数据集与隐私说明

该目录包含学生心理评估微调所需的公开数据与元信息，已去除可识别个人信息，仅保留对话内容与标签。若需访问完整原始版本，请遵循 README 顶层的获取指南。

## 目录结构

- `datasets/train/train.jsonl`：80% 训练集，JSONL/Alpaca 格式。
- `datasets/valid/valid.jsonl`：10% 验证集，用于调参与早停。
- `datasets/test/test.jsonl`：10% 测试集，仅用于最终评估。
- `datasets/metadata/dataset_info.json`：字段映射、格式与标签定义。
- `processed/`：存放清洗后的派生数据或特征（当前留空，可在运行 `scripts/prepare_sft_dataset.py` 后写入）。

## 使用方式

1. 若需要增量标注/清洗，可在 `processed/` 下创建新文件，并在仓库根 README 中补充步骤。
2. 任何包含真实学生信息的扩展数据必须在提交前脱敏；建议使用 `labeling/scripts/split_jsonl.py` 进行拆分与抽样，再人工检查。
3. 如需共享完整原始集，请通过私有渠道发送，并在 GitHub Issue 中附带审核记录而非直接上传。

## 隐私与合规

- 数据已移除姓名、学校、联系方式等直接身份标识，但仍含心理咨询对话，分享时需遵守所在地隐私法规。
- 建议在发布 release 前，再次执行自动扫描（例如 `grep` 敏感词）与人工抽检。
- 若发现潜在敏感内容，请在提交 PR 前删除相关片段并更新本文件的“使用方式”章节。
