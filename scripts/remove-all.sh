#!/usr/bin/env bash
set -euo pipefail

# Força a não conversão de paths no Windows Git Bash
export MSYS_NO_PATHCONV=1

# Lista de runtimes
RUNTIMES=(python ruby java dotnet)

echo "=== Removendo todas as funções Serverless ==="

for runtime in "${RUNTIMES[@]}"; do
  echo ""
  echo ">>> Removendo função: $runtime"
  (
    cd "../$runtime"
    sls remove
  )
done

echo ""
echo "=== Remoção concluída com sucesso para todos os runtimes ==="
