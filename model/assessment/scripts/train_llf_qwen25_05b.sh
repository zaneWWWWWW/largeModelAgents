#!/bin/bash
#SBATCH --job-name=llf_qwen25_05b_lora
#SBATCH --output=llf_qwen25_05b_lora_%j.out
#SBATCH --error=llf_qwen25_05b_lora_%j.err
#SBATCH --partition=A6000
#SBATCH --nodes=1
#SBATCH --cpus-per-task=8
#SBATCH --mem=32G
#SBATCH --gres=gpu:1
#SBATCH --time=7-00:00:00
#SBATCH --chdir=/share/home/gpu093197/zanewang/project-llm

set -euo pipefail
source /share/home/gpu093197/anaconda3/etc/profile.d/conda.sh 2>/dev/null || true
conda activate llama-zanewang 2>/dev/null || true
unset LD_PRELOAD 2>/dev/null || true
export HF_ENDPOINT="https://huggingface.co"
export HF_HUB_ENABLE_HF_TRANSFER=1
export HF_HOME="/share/home/gpu093197/.cache/huggingface"
if [ -f "/etc/pki/tls/certs/ca-bundle.crt" ]; then
    export REQUESTS_CA_BUNDLE=/etc/pki/tls/certs/ca-bundle.crt
    export CURL_CA_BUNDLE=/etc/pki/tls/certs/ca-bundle.crt
elif [ -f "/etc/ssl/certs/ca-certificates.crt" ]; then
    export REQUESTS_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt
    export CURL_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt
fi
echo "CUDA_VISIBLE_DEVICES=$CUDA_VISIBLE_DEVICES"
nvidia-smi || true
PYTHON=${PYTHON:-python}
LLF_PY=/share/home/gpu093197/zanewang/project-llm/LLaMA-Factory-main
export PYTHONPATH="$LLF_PY/src:${PYTHONPATH:-}"
${PYTHON} -c "import torch; print('torch:', getattr(torch, '__version__', 'N/A'), 'cuda:', torch.cuda.is_available())" || { echo 'ERROR: torch not available in current environment'; exit 1; }
${PYTHON} -m llamafactory.cli train /share/home/gpu093197/zanewang/project-llm/llamafactory/configs/train_qwen25_05b_lora.yaml

