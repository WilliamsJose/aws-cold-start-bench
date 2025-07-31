# Serverless Cold Start Benchmark

Este projeto foi criado para **comparar o tempo de cold start** em diferentes runtimes do AWS Lambda, usando o **Serverless Framework (~4.0)**.  
Os runtimes suportados neste benchmark são:

- **Node.js 22.x**
- **Python 3.11**
- **Ruby 3.2**
- **Java 17**
- **.NET 8 (C#)**

---

## Estrutura do Projeto

├── serverless-coldstart/

│ ├── node/

│ ├── python/

│ ├── ruby/

│ ├── public/

│ ├── java/

│ ├── dotnet/

│ ├── scripts/ -> Scripts auxiliares (deploy e testes)

├── README.md

└── .gitignore

Cada pasta é **um projeto Serverless independente**, permitindo controle separado de build e deploy.

---

## Pré-requisitos

1. **AWS CLI** configurado com o `profile` **awsDevelopment**

```bash
aws configure --profile awsDevelopment

Serverless Framework instalado globalmente:

npm install -g serverless

Docker (apenas se for usar os scripts de build no Windows)
```

## Deploy

2. Para fazer o deploy de todos os runtimes de uma vez, rode o script:

```bash
chmod +x scripts/deploy-all.sh
./scripts/deploy-all.sh
```

O script entra em cada pasta e roda sls deploy.

Você também pode fazer o deploy manualmente:

_cd node && sls deploy_

## Testando os Endpoints

Após o deploy, cada função terá um endpoint HTTP no formato:

_https://< api-id >.execute-api.us-east-1.amazonaws.com/<runtime>/hello_

Para testar todos de uma vez:

```bash
chmod +x scripts/curl-bench.sh
./scripts/curl-bench.sh
```

<small>Para funcionar deve editar o arquivo curl-bench.sh com todos endpoints.</small>

Saída esperada:

```json
Testando https://.../node/hello
{
  "message": "HELLO WORLD",
  "runtime": "nodejs22.x",
  "coldStart": true,
  "handlerDurationMs": 5
}
```

---

O script usa curl e opcionalmente o jq para formatar o JSON (instale com choco install jq no Windows).

## Como funciona

Cada Lambda retorna um JSON com as mesmas chaves:

```json
{
  "message": "HELLO WORLD",
  "runtime": "nodejs22.x",
  "coldStart": true, // identifica se é a primeira execução do container.
  "handlerDurationMs": 5 // tempo em milissegundos gasto apenas dentro do handler.
}
```

## Por que usar este benchmark?

Medir o impacto do cold start em diferentes linguagens.
Comparar a performance de runtimes em configurações iguais:

    memorySize: 1024 MB
    architecture: arm64
    timeout: 5 segundos

## Resultados Cloudwatch

![Tempo de execução de cada função](<public/Captura de tela 2025-07-30 212411.png>)
![Tamanho final do código empacotado](<public/Captura de tela 2025-07-30 212505.png>)

## Resultados no terminal

```bash
$ ./curl-bench.sh
Testando https://waxg68p0u8.execute-api.us-east-1.amazonaws.com/node/hello
{
  "message": "HELLO WORLD",
  "runtime": "nodejs22.x",
  "coldStart": true,
  "handlerDurationMs": 0
}

real    0m1,182s
user    0m0,000s
sys     0m0,031s
---------------------
Testando https://lnfi1t6jog.execute-api.us-east-1.amazonaws.com/python/hello
{
  "message": "HELLO WORLD",
  "runtime": "python3.11",
  "coldStart": true,
  "handlerDurationMs": 0
}

real    0m1,003s
user    0m0,015s
sys     0m0,015s
---------------------
Testando https://y087r5gxxc.execute-api.us-east-1.amazonaws.com/ruby/hello
{
  "message": "HELLO WORLD",
  "runtime": "ruby3.2",
  "coldStart": true,
  "handlerDurationMs": 0
}

real    0m1,285s
user    0m0,015s
sys     0m0,015s
---------------------
Testando https://gkr6ll70dh.execute-api.us-east-1.amazonaws.com/java/hello
{
  "message": "HELLO WORLD",
  "runtime": "java17",
  "coldStart": true,
  "handlerDurationMs": 0
}

real    0m1,450s
user    0m0,031s
sys     0m0,031s
---------------------
Testando https://76ylb93uwa.execute-api.us-east-1.amazonaws.com/dotnet/hello
{
  "message": "HELLO WORLD",
  "runtime": "dotnet8",
  "coldStart": true,
  "handlerDurationMs": 0
}

real    0m1,719s
user    0m0,015s
sys     0m0,000s
---------------------
```
