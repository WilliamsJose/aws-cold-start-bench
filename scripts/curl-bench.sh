#!/usr/bin/env bash
set -euo pipefail

# Força conversão correta de URLs e timing no Git Bash/Windows
export MSYS_NO_PATHCONV=1

NODE_URL="${NODE_URL:-https://<api-id>.execute-api.us-east-1.amazonaws.com/node/hello}"
PY_URL="${PY_URL:-https://<api-id>.execute-api.us-east-1.amazonaws.com/python/hello}"
RB_URL="${RB_URL:-https://<api-id>.execute-api.us-east-1.amazonaws.com/ruby/hello}"
JAVA_URL="${JAVA_URL:-https://<api-id>.execute-api.us-east-1.amazonaws.com/java/hello}"
DOTNET_URL="${DOTNET_URL:-https://<api-id>.execute-api.us-east-1.amazonaws.com/dotnet/hello}"

for url in "$NODE_URL" "$PY_URL" "$RB_URL" "$JAVA_URL" "$DOTNET_URL"; do
  echo "Testando $url"
  
  # Usa comando time embutido do bash, não o do Windows
  { time curl -s "$url" | jq .; } 2>&1
  
  echo "---------------------"
done
