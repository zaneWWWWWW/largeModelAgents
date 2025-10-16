import argparse
from pathlib import Path
import yaml


def train(config: dict):
    print("[Assessment] Training with config:")
    print(config)
    # TODO: 实现评分/分类模型训练流程（特征工程、监督学习、校验等）


def load_config(config_path: Path) -> dict:
    with open(config_path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--config",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "configs" / "default.yaml",
    )
    args = parser.parse_args()
    cfg = load_config(args.config)
    train(cfg)