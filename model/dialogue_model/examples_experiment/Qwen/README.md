---
library_name: peft
license: other
base_model: /root/autodl-tmp/Qwen/Gemma
tags:
- base_model:adapter:/root/autodl-tmp/Qwen/Gemma
- llama-factory
- lora
- transformers
pipeline_tag: text-generation
model-index:
- name: sft
  results: []
---

<!-- This model card has been generated automatically according to the information the Trainer had access to. You
should probably proofread and complete it, then remove this comment. -->

# sft

This model is a fine-tuned version of [/root/autodl-tmp/Qwen/Gemma](https://huggingface.co//root/autodl-tmp/Qwen/Gemma) on the train_data_multi_50pct and the merged_30pct datasets.
It achieves the following results on the evaluation set:
- Loss: 2.6625

## Model description

More information needed

## Intended uses & limitations

More information needed

## Training and evaluation data

More information needed

## Training procedure

### Training hyperparameters

The following hyperparameters were used during training:
- learning_rate: 0.0001
- train_batch_size: 2
- eval_batch_size: 2
- seed: 42
- gradient_accumulation_steps: 8
- total_train_batch_size: 16
- optimizer: Use adamw_torch with betas=(0.9,0.999) and epsilon=1e-08 and optimizer_args=No additional optimizer arguments
- lr_scheduler_type: cosine
- lr_scheduler_warmup_ratio: 0.1
- num_epochs: 3.0

### Training results

| Training Loss | Epoch  | Step  | Validation Loss |
|:-------------:|:------:|:-----:|:---------------:|
| 3.2438        | 0.1365 | 500   | 3.0463          |
| 3.0954        | 0.2730 | 1000  | 2.9459          |
| 3.1269        | 0.4096 | 1500  | 2.8809          |
| 2.8184        | 0.5461 | 2000  | 2.8389          |
| 2.932         | 0.6826 | 2500  | 2.8076          |
| 2.9615        | 0.8191 | 3000  | 2.7835          |
| 2.7747        | 0.9557 | 3500  | 2.7625          |
| 2.6437        | 1.0920 | 4000  | 2.7474          |
| 2.8564        | 1.2285 | 4500  | 2.7339          |
| 2.6849        | 1.3651 | 5000  | 2.7256          |
| 2.4931        | 1.5016 | 5500  | 2.7116          |
| 2.7212        | 1.6381 | 6000  | 2.6992          |
| 2.7392        | 1.7746 | 6500  | 2.6897          |
| 2.6921        | 1.9112 | 7000  | 2.6790          |
| 2.6111        | 2.0475 | 7500  | 2.6793          |
| 2.6458        | 2.1840 | 8000  | 2.6731          |
| 2.6233        | 2.3206 | 8500  | 2.6708          |
| 2.7112        | 2.4571 | 9000  | 2.6668          |
| 2.5479        | 2.5936 | 9500  | 2.6654          |
| 2.3845        | 2.7301 | 10000 | 2.6633          |
| 2.7159        | 2.8667 | 10500 | 2.6627          |


### Framework versions

- PEFT 0.17.1
- Transformers 4.57.1
- Pytorch 2.5.1+cu121
- Datasets 4.0.0
- Tokenizers 0.22.1