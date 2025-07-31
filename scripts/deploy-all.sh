#!/usr/bin/env bash
set -euo pipefail

# Força a não conversão de paths no Windows Git Bash
export MSYS_NO_PATHCONV=1

# Lista de runtimes
RUNTIMES=(node python ruby java dotnet)

echo "=== Deploy de todas as funções Serverless ==="

for runtime in "${RUNTIMES[@]}"; do
  echo ""
  echo ">>> Deploy da função: $runtime"
  (
    cd "../$runtime"
    sls deploy
  )
done

echo ""
echo "=== Deploy concluído com sucesso para todos os runtimes ==="
