# Assessment Model

This directory contains training and inference for mental health status judgement and questionnaire scoring/categorization.

## Directory
- `configs/`: model and scoring related configurations.
- `training/`: training scripts.
- `evaluation/`: evaluation metrics and scripts.
- `inference/`: inference and service adaptation.
- `data/`: data directory (gitignored).

## Run Examples
```bash
python training/train.py --config configs/default.yaml
python evaluation/evaluate.py --config configs/default.yaml
```

---

# 心理评测模型（中文）

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