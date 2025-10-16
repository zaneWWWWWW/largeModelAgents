# 心理评测模型（Assessment Model）

本目录用于心理健康状态判断与量表评分/分级的训练与推理。

## 目录说明
- `configs/`：模型与评分相关配置。
- `training/`：训练脚本。
- `evaluation/`：评测指标与脚本。
- `inference/`：推理与服务化适配。
- `data/`：数据目录（已 gitignore）。

## 运行示例
```bash
python training/train.py --config configs/default.yaml
python evaluation/evaluate.py --config configs/default.yaml
```