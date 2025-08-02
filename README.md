# 🚀 AWS Lambda Cold Start Benchmark - Versão Aprimorada

Este projeto foi **completamente refatorado** para fornecer **medições precisas e comparações justas** de cold start em diferentes runtimes do AWS Lambda, usando o **Serverless Framework (~4.0)**.

## 🎯 Runtimes Suportados

- **Node.js 22.x** - Com timing de alta precisão e métricas de sistema
- **Python 3.11** - Com psutil para métricas detalhadas de memória
- **Ruby 3.2** - Com timing monotônico e informações de sistema
- **Java 21** - Com records, collections imutáveis e features modernas
- **.NET 8 (C#)** - Com Stopwatch de alta precisão e métricas de GC

## ✨ Melhorias Implementadas

### 🎯 **Timing de Alta Precisão**
- **Antes**: Medição simples apenas do handler
- **Agora**: Múltiplos pontos de timing com precisão de 2 casas decimais
- Captura: inicialização do módulo, execução do handler, trabalho específico, idade do container

### 🧠 **Detecção Robusta de Cold Start**
- **Antes**: Apenas primeira invocação (`isCold = true`)
- **Agora**: Detecta cold starts após 5+ minutos de inatividade
- Rastreia contador de invocações e idade real do container

### 📊 **Formato Padronizado**
- Resposta JSON consistente entre todas as linguagens
- Headers HTTP informativos (`X-Cold-Start`, `X-Handler-Duration-Ms`, etc.)
- Logging estruturado para CloudWatch

### 📈 **Métricas Detalhadas**
- Uso de memória (heap, RSS, working set)
- Informações de sistema (CPU, plataforma, versão runtime)
- Lifecycle do container com timestamps

---

## 📁 Estrutura do Projeto

```
aws-cold-start-test/
├── node/                    # Node.js 22.x handler aprimorado
├── python/                  # Python 3.11 + psutil
├── ruby/                    # Ruby 3.2 com timing monotônico
├── java/                    # Java 21 com records e features modernas
├── dotnet/                  # .NET 8 com Stopwatch de alta precisão
├── scripts/                 # Scripts de build e deploy
├── IMPROVEMENTS.md          # Documentação detalhada das melhorias
└── README.md               # Este arquivo
```

Cada pasta é **um projeto Serverless independente** com:
- ✅ Handler completamente refatorado
- ✅ Timing de alta precisão
- ✅ Detecção robusta de cold start
- ✅ Métricas detalhadas de sistema
- ✅ Logging estruturado

---

## 🛠️ Pré-requisitos

### **Essenciais**
```bash
# 1. AWS CLI configurado
aws configure --profile awsDevelopment

# 2. Serverless Framework
npm install -g serverless

# 3. Docker (para compilação Java)
docker --version
```

### **Dependências por Linguagem**
- **Node.js**: Nativo (performance, os)
- **Python**: `psutil==5.9.8` (adicionado automaticamente)
- **Ruby**: Nativo (Process, Etc)
- **Java**: Maven + Docker para compilação
- **.NET**: Nativo (Stopwatch, Process)

---

## 🚀 Build e Deploy

### **Build Automatizado**
```bash
# Compila todos os projetos (exceto .NET)
chmod +x scripts/build-all.sh
./scripts/build-all.sh
```

### **Deploy Todos os Runtimes**
```bash
# Deploy completo
chmod +x scripts/deploy-all.sh
./scripts/deploy-all.sh
```

### **Deploy Individual**
```bash
# Exemplo: apenas Node.js
cd node
sls deploy

# Exemplo: apenas Java 21
cd java
sls deploy
```

---

## 📊 Como Usar e Analisar Resultados

### **Testando as Funções**
```bash
# Após deploy, teste cada runtime:
curl https://your-api-gateway-url/node/hello
curl https://your-api-gateway-url/python/hello
curl https://your-api-gateway-url/ruby/hello
curl https://your-api-gateway-url/java/hello
curl https://your-api-gateway-url/dotnet/hello
```

### **Exemplo de Resposta Padronizada**
```json
{
  "message": "HELLO WORLD",
  "runtime": "java21",
  "timestamp": "2025-08-02T21:06:00.123Z",
  "requestId": "abc123-def456",
  "coldStart": true,
  "invocationCount": 1,
  "timings": {
    "moduleInitDurationMs": 0.15,
    "handlerDurationMs": 2.34,
    "workDurationMs": 0.05,
    "totalContainerAgeMs": 0.15
  },
  "system": {
    "memoryUsage": {
      "heapUsed": 45.67,
      "heapMax": 512.0
    },
    "cpuCount": 2,
    "platform": "Linux",
    "arch": "x64",
    "javaVersion": "21.0.1"
  },
  "container": {
    "initTimestamp": "2025-08-02T21:06:00.000Z",
    "ageMs": 0.15
  }
}
```

### **Headers HTTP Informativos**
```
X-Cold-Start: true
X-Invocation-Count: 1
X-Handler-Duration-Ms: 2.34
X-Runtime: java21
```

### **Análise Comparativa**

#### **Métricas Principais para Comparar:**
1. **`moduleInitDurationMs`** - Overhead de inicialização (cold start real)
2. **`handlerDurationMs`** - Performance total da função
3. **`system.memoryUsage`** - Consumo de memória por runtime
4. **`coldStart`** - Detecção precisa de cold starts

#### **Logs Estruturados no CloudWatch**
```json
{
  "level": "INFO",
  "timestamp": "2025-08-02T21:06:00.123Z",
  "requestId": "abc123-def456",
  "coldStart": true,
  "invocationCount": 1,
  "timings": {
    "handlerDurationMs": 2.34,
    "moduleInitDurationMs": 0.15
  },
  "memoryUsage": 45.67,
  "message": "Lambda invocation completed"
}
```

#### **Queries CloudWatch Úteis**
```sql
-- Cold starts por runtime
fields @timestamp, runtime, coldStart, timings.moduleInitDurationMs
| filter coldStart = true
| sort @timestamp desc

-- Performance comparison
fields runtime, timings.handlerDurationMs, system.memoryUsage
| stats avg(timings.handlerDurationMs) by runtime
```

---

## 🎯 Principais Diferenças das Melhorias

| Aspecto | Antes | Depois |
|---------|-------|--------|
| **Timing** | Handler simples | Múltiplos pontos de alta precisão |
| **Cold Start** | Apenas 1ª invocação | Detecção após inatividade (5min) |
| **Métricas** | Básicas | Sistema completo + memória |
| **Formato** | Inconsistente | Padronizado entre linguagens |
| **Logging** | Simples | Estruturado para análise |
| **Headers** | Básicos | Informativos para automação |
| **Java** | 17 | 21 com records e features modernas |

---

## 📚 Documentação Adicional

- **[IMPROVEMENTS.md](./IMPROVEMENTS.md)** - Detalhes técnicos das melhorias
- **[java/JAVA21_FEATURES.md](./java/JAVA21_FEATURES.md)** - Features específicas do Java 21
- **Logs CloudWatch** - Análise em tempo real das métricas

---

## 🤝 Contribuindo

Este projeto agora oferece:
- ✅ **Medições precisas** de cold start
- ✅ **Comparações justas** entre linguagens
- ✅ **Métricas detalhadas** para análise
- ✅ **Formato padronizado** para automação
- ✅ **Logging estruturado** para monitoramento

**Próximos passos sugeridos:**
1. Dashboard para visualização das métricas
2. Testes automatizados de carga
3. Análise estatística (percentis, distribuições)
4. Otimizações baseadas nos dados coletados

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

## Resultados (em breve)

_Esta seção será atualizada com novos benchmarks._
