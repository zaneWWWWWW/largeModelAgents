Param(
  [string]$Path = ".venv"
)

Write-Host "[setup] Creating virtual environment at $Path"
python -m venv $Path

$activate = Join-Path $Path "Scripts\Activate.ps1"
if (Test-Path $activate) {
  Write-Host "[setup] Virtual env created. To activate: `n$activate"
} else {
  Write-Warning "[setup] Activation script not found: $activate"
}

Write-Host "[setup] Installing API dependencies"
pip install --upgrade pip
pip install -r develop/services/api/requirements.txt
Write-Host "[setup] Done. Activate venv then run: uvicorn main:app --reload --port 8000 (in develop/services/api)"