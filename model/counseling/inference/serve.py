from typing import Dict


class PlaceholderModel:
    def __init__(self, config: Dict | None = None):
        self.config = config or {}

    def reply(self, user_message: str) -> str:
        # TODO: 替换为真实推理逻辑
        return "你好，我是你的心理咨询助理。这是占位回复，后续将接入真实模型。"


def load_model(config: Dict | None = None) -> PlaceholderModel:
    return PlaceholderModel(config)