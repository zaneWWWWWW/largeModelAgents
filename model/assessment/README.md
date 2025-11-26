# Fine-Tuning Project for Student Mental Health Chatbot

This repository contains the code, data, and models for a fine-tuned language model designed to act as a supportive chatbot for student mental health.

## Directory Structure

- `LLaMA-Factory-main/`: The core fine-tuning framework used for this project.
- `data/`: Contains the dataset used for fine-tuning.
  - `datasets/sft_student_mental.jsonl`: The instruction-based dataset for supervised fine-tuning.
- `models/`: Contains the quantized model ready for inference.
  - `mindchat-qwen2-05b-merged-gguf/`: The GGUF-quantized model for efficient deployment.
- `saves/`: Contains the fine-tuned model weights (adapters).
  - `mindchat-qwen2-05b/`: The LoRA adapter weights from the fine-tuning process.
- `docs/`: Project documentation.
  - `finetune_plan.md`: The initial plan for the fine-tuning experiment.
  - `steps.md`: Detailed steps taken during the project.
  - `README.md`: The original project README.
- `logs/`: Training logs.

## How to Use

1.  **Setup**: Follow the instructions in `LLaMA-Factory-main/README.md` to set up the environment.
2.  **Inference**: Use the provided GGUF model in `models/` with a compatible runner like `llama.cpp` for fast inference.
3.  **Reproduce Training**: To reproduce the training, place the `sft_student_mental.jsonl` dataset in the `LLaMA-Factory-main/data` directory and use the LoRA weights in `saves/` as a starting point or retrain using the provided scripts and logs as a reference.
