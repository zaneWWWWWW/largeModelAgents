# 心理咨询模型（Counseling Model）

本目录用于心理咨询对话模型的训练、评估与推理。当前为骨架占位，后续依据选型（如指令微调、检索增强、对齐训练等）完善。

## 目录说明
- `configs/`：训练与推理相关配置（YAML）。
- `training/`：训练脚本与管线。
- `evaluation/`：评估脚本与指标。
- `inference/`：推理与部署适配代码。
- `data/`：本地数据目录（已 gitignore）。

## 运行示例
```bash
python training/train.py --config configs/default.yaml
python evaluation/evaluate.py --config configs/default.yaml
```