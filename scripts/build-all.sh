#!/usr/bin/env bash
set -euo pipefail

# Evita conversão de caminhos no Git Bash
export MSYS_NO_PATHCONV=1

# Caminho base (volta um nível da pasta scripts)
BASE_PATH="$(cd "$(dirname "$0")/.." && pwd)"

echo "===> Build Java (Maven)..."
docker run --rm \
  -v "${BASE_PATH}/java":/app \
  -w /app \
  maven:3.9.4-eclipse-temurin-17 \
  mvn clean package -DskipTests

echo "===> Build .NET (dotnet publish)..."
docker run --rm \
  -v "${BASE_PATH}/dotnet":/src \
  -w /src \
  mcr.microsoft.com/dotnet/sdk:8.0 \
  dotnet publish -c Release -o /src/publish

echo "===> Empacotando .NET function.zip..."
(
  cd "${BASE_PATH}/dotnet/publish"
  
  # Usa PowerShell para compactar (já vem no Windows)
  powershell.exe -Command "Compress-Archive -Path * -DestinationPath ..\\function.zip -Force"
)

# Remove pasta publish (já empacotada)
rm -rf "${BASE_PATH}/dotnet/publish"

echo "===> Build concluído com sucesso!"
