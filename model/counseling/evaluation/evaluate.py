import argparse
from pathlib import Path
import yaml


def evaluate(config: dict):
    print("[Counseling] Evaluating with config:")
    print(config)
    # TODO: 实现指标计算（如安全性、同理心、连贯性、对话质量等）


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
    evaluate(cfg)