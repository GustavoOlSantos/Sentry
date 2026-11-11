# Sentry

Plataforma para **descoberta de ativos, identificação de vulnerabilidades e avaliação de configurações de segurança** em ambientes de rede controlados e autorizados.

<div align="center">

<img src="https://skillicons.dev/icons?i=java,spring,react,javascript,rabbitmq,mysql,docker" />

</div>

---

## Sobre o projeto

O **Sentry** é um projeto acadêmico desenvolvido como Trabalho de Conclusão de Curso (TCC), com foco em **desenvolvimento e segurança da informação**.

A plataforma tem como objetivo centralizar o processo de descoberta e análise de ativos de rede, permitindo:

- realizar varreduras de um ou mais alvos;
- descobrir hosts, portas e serviços;
- identificar versões e informações CPE;
- correlacionar CPEs com vulnerabilidades conhecidas;
- avaliar configurações de segurança;
- aplicar regras e políticas de segurança;
- organizar vulnerabilidades e violações em **Findings**;
- acompanhar o ciclo de vida dos achados;
- atribuir responsáveis pela resolução;
- consultar o histórico das varreduras e dos ativos encontrados.

O **Nmap** é utilizado como mecanismo de descoberta e coleta técnica. A consulta ao **NVD (National Vulnerability Database)** permite correlacionar os componentes identificados com vulnerabilidades conhecidas.

O processamento das varreduras é realizado de forma **assíncrona através do RabbitMQ**, permitindo que múltiplos alvos sejam processados independentemente.

> [!WARNING]
> O Sentry é um projeto acadêmico e deve ser utilizado exclusivamente em máquinas, redes e serviços para os quais exista autorização explícita para realização de testes.

---

## Objetivos

### Objetivo geral

Desenvolver uma plataforma capaz de auxiliar na identificação, organização e acompanhamento de riscos de segurança encontrados em ativos de rede.

### Objetivos específicos

- Executar varreduras de um ou mais alvos autorizados.
- Processar os alvos de forma assíncrona utilizando RabbitMQ.
- Identificar hosts, portas, serviços e versões.
- Extrair informações CPE fornecidas pelo Nmap.
- Consultar o NVD para identificar vulnerabilidades relacionadas aos CPEs.
- Detectar configurações potencialmente inseguras.
- Aplicar regras de políticas de segurança.
- Normalizar vulnerabilidades e violações em objetos `Finding`.
- Persistir ativos, serviços, varreduras, vulnerabilidades e findings.
- Controlar o ciclo de vida dos findings.
- Permitir atribuição de responsáveis aos achados.
- Disponibilizar autenticação e controle de acesso.
- Disponibilizar uma interface web para visualização e gerenciamento dos resultados.

---

## Funcionalidades

### 🔎 Varredura e descoberta

- Varredura de múltiplos alvos.
- Validação dos alvos informados.
- Execução do Nmap como processo externo.
- Timeout para evitar processos de varredura indefinidamente executando.
- Parsing do XML produzido pelo Nmap.
- Identificação de:
  - hosts;
  - portas;
  - estados das portas;
  - serviços;
  - versões;
  - sistema operacional;
  - CPEs.

### 🐇 Processamento assíncrono

As solicitações de varredura são processadas através do RabbitMQ.

O sistema utiliza:

- publicação assíncrona;
- confirmação de publicação;
- tratamento de mensagens não confirmadas;
- retry para falhas transitórias;
- distinção entre falhas transitórias e permanentes;
- tratamento de falhas de processamento;
- agrupamento de execuções através de `ScanExecutionGroup`.

Cada alvo pode ser processado independentemente, permitindo que uma solicitação contendo múltiplos alvos seja distribuída para processamento.

### 🛡️ Vulnerabilidades

- Consulta ao NVD API 2.0.
- Busca de vulnerabilidades através de CPE.
- Mapeamento das respostas do NVD para o domínio da aplicação.
- Persistência das vulnerabilidades identificadas.
- Associação das vulnerabilidades aos ativos e serviços encontrados.

### 📋 Políticas de segurança

O sistema possui uma estrutura para definição e avaliação de políticas e regras de segurança.

Entre as verificações implementadas estão:

- certificados expirados;
- portas de bancos de dados expostas;
- SSH exposto;
- regras baseadas em portas e serviços.

A arquitetura permite a inclusão de novas verificações através do modelo de `ConfigCheck` e da avaliação de `PolicyRule`.

### 🚨 Findings

Vulnerabilidades e violações de políticas são normalizadas em um modelo único de `Finding`.

Cada finding pode conter informações como:

- ativo relacionado;
- serviço;
- vulnerabilidade;
- regra de política;
- tipo;
- severidade;
- status;
- título;
- descrição;
- evidências;
- data de detecção;
- responsável.

O sistema também possui mecanismo de **reconciliação de findings**, permitindo evitar a criação indiscriminada de registros duplicados entre diferentes execuções.

### 👤 Autenticação e usuários

O sistema possui autenticação baseada em **JWT** e controle de acesso através do Spring Security.

Inclui:

- login;
- emissão de JWT;
- controle de roles;
- gerenciamento de usuários;
- criação e remoção de usuários;
- atualização de dados;
- alteração de senha;
- políticas de senha;
- administrador inicial configurável.

### 🖥️ Interface web

O frontend é desenvolvido em React e possui estrutura para:

- autenticação;
- dashboard;
- visualização de ativos;
- execução de scans;
- visualização de findings;
- gerenciamento de usuários;
- gerenciamento de políticas;
- navegação protegida;
- tratamento de páginas não encontradas e acesso não autorizado.

---

## Arquitetura

O fluxo principal do sistema pode ser representado da seguinte forma:

```text
┌──────────────────────┐
│      React / Web     │
└──────────┬───────────┘
           │ HTTP / REST
           ▼
┌──────────────────────┐
│   Spring Boot API    │
│                      │
│ Auth / Scan / Asset  │
│ Finding / Policy     │
└──────────┬───────────┘
           │
           ├───────────────────────┐
           │                       │
           ▼                       ▼
┌──────────────────┐     ┌──────────────────┐
│     MySQL        │     │    RabbitMQ      │
│                  │     │                  │
│ Scans            │     │ Scan messages    │
│ Assets           │     │ Retry / Queue    │
│ Services         │     └────────┬─────────┘
│ Findings         │              │
│ Vulnerabilities  │              ▼
│ Policies         │     ┌──────────────────┐
│ Users            │     │  Scan Consumer   │
└──────────────────┘     └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │      Nmap        │
                         └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │ Nmap XML Parser  │
                         └────────┬─────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
             ┌──────────────┐          ┌────────────────┐
             │   Policies   │          │      NVD       │
             │  Evaluation  │          │  Vulnerability │
             └──────┬───────┘          │    Lookup      │
                    │                  └───────┬────────┘
                    └──────────┬───────────────┘
                               ▼
                       ┌────────────────┐
                       │    Findings    │
                       └────────────────┘
```

### Fluxo de uma varredura

```text
POST /scan/exec
       │
       ▼
ScanService
       │
       ├── valida os alvos
       ├── remove duplicidades
       ├── cria ScanExecutionGroup
       ├── cria os Scans
       └── publica mensagens
                │
                ▼
             RabbitMQ
                │
                ▼
          ScanConsumer
                │
                ▼
          NmapScanEngine
                │
        ┌───────┴────────┐
        ▼                ▼
   Nmap Parser       Nmap timeout
        │
        ▼
   Assets / Services
        │
        ├───────────────┐
        ▼               ▼
 Policy Evaluation    NVD Lookup
        │               │
        └───────┬───────┘
                ▼
             Findings
                │
                ▼
       Finding Reconciliation
                │
                ▼
            MySQL
```

A API não precisa aguardar o término da varredura para responder à solicitação. O processamento acontece de maneira assíncrona e os resultados podem ser consultados posteriormente.

---

## Persistência

O sistema utiliza **MySQL** com **Spring Data JPA / Hibernate**.

Entre as principais entidades persistidas estão:

```text
User
  │
  └── autenticação / autorização

ScanExecutionGroup
  │
  └── Scan
       │
       └── ScanAsset
             │
             └── Asset
                  │
                  └── Service

Asset
  │
  └── Finding
        ├── Vulnerability
        └── PolicyRule
```

A associação `ScanAsset` representa a relação entre uma execução de scan e um ativo descoberto, permitindo manter informações específicas daquela descoberta.

---

## Tecnologias

### Backend

- **Java 21**
- **Spring Boot 4.1.1**
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Hibernate
- Spring AMQP
- Spring RestClient
- Spring Cache
- Spring Validation
- Spring Actuator
- Springdoc OpenAPI
- Lombok
- Maven Wrapper

### Frontend

- **React 19**
- **Vite 8**
- React Router
- Axios
- React Select
- JavaScript

### Banco de dados

- **MySQL 9.7**

### Mensageria

- **RabbitMQ**

### Segurança

- Spring Security
- JWT
- OAuth2 Resource Server / Nimbus

### Ferramentas e integrações

- **Nmap**
- **NVD API 2.0**
- Docker
- Docker Compose

---

## Estrutura do projeto

```text
sentry/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/br/uva/tcc/sentry/
│   │   │   │
│   │   │   ├── Asset/
│   │   │   │   ├── controller/
│   │   │   │   ├── DTO/
│   │   │   │   ├── domain/
│   │   │   │   ├── repository/
│   │   │   │   └── service/
│   │   │   │
│   │   │   ├── auth/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── DTO/
│   │   │   │   ├── domain/
│   │   │   │   ├── exception/
│   │   │   │   ├── repository/
│   │   │   │   ├── security/
│   │   │   │   └── service/
│   │   │   │
│   │   │   ├── config/
│   │   │   │
│   │   │   ├── finding/
│   │   │   │   ├── controller/
│   │   │   │   ├── DTO/
│   │   │   │   ├── domain/
│   │   │   │   ├── exception/
│   │   │   │   ├── factory/
│   │   │   │   ├── repository/
│   │   │   │   └── service/
│   │   │   │
│   │   │   ├── policy/
│   │   │   │   ├── checker/
│   │   │   │   ├── controller/
│   │   │   │   ├── domain/
│   │   │   │   ├── repository/
│   │   │   │   └── service/
│   │   │   │
│   │   │   ├── scan/
│   │   │   │   ├── controller/
│   │   │   │   ├── DTO/
│   │   │   │   ├── domain/
│   │   │   │   ├── exception/
│   │   │   │   ├── messaging/
│   │   │   │   ├── repository/
│   │   │   │   ├── scanner/
│   │   │   │   ├── services/
│   │   │   │   └── validation/
│   │   │   │
│   │   │   ├── shared/
│   │   │   │
│   │   │   └── vulnerability/
│   │   │       ├── domain/
│   │   │       ├── nvd/
│   │   │       └── repository/
│   │   │
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── logback-spring.xml
│   │
│   ├── src/test/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
│
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── app/
│   │   │   └── routes/
│   │   ├── assets/
│   │   ├── components/
│   │   └── ...
│   ├── package.json
│   └── vite.config.js
│
├── lab-hosts/
│   └── lab-docker-compose.yml
│
├── README.md
└── dev-README.md
```

---

## Requisitos

Para executar o projeto localmente, é necessário possuir:

- JDK 21;
- Node.js e npm;
- Docker;
- Nmap instalado e disponível no `PATH`;
- acesso à internet;
- MySQL;
- RabbitMQ.

O Maven não precisa estar instalado globalmente, pois o projeto possui **Maven Wrapper**.

---

## Configuração

### Variáveis de ambiente

As principais configurações podem ser sobrescritas através de variáveis de ambiente.

| Variável | Padrão | Descrição |
|---|---|---|
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Porta do RabbitMQ |
| `RABBITMQ_USERNAME` | `guest` | Usuário do RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Senha do RabbitMQ |
| `NVD_API_KEY` | — | Chave opcional da API do NVD |
| `NMAP_TIMEOUT_SECONDS` | `120` | Timeout de uma execução do Nmap |
| `SENTRY_JWT_SECRET` | — | Chave utilizada para assinatura dos JWTs |
| `SENTRY_JWT_EXPIRATION` | `PT2H` | Tempo de expiração do JWT |
| `SENTRY_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Origens permitidas pelo CORS |
| `SENTRY_PUBLIC_DOCS` | `true` | Disponibilidade pública da documentação |
| `SENTRY_ADMIN_NAME` | `Administrador` | Nome do administrador inicial |
| `SENTRY_ADMIN_EMAIL` | `admin@sentry.com` | E-mail do administrador inicial |
| `SENTRY_ADMIN_PASSWORD` | `12345678` | Senha do administrador inicial |

> [!IMPORTANT]
> Em ambientes reais, não utilize as credenciais padrão. Principalmente `SENTRY_JWT_SECRET`, senha do banco e credenciais do administrador devem ser configuradas através de variáveis de ambiente ou outro mecanismo seguro de gerenciamento de segredos.

### NVD API

A chave do NVD é opcional, porém recomendada.

Sem chave, as consultas continuam sendo realizadas dentro do limite público da API.

É possível solicitar uma chave gratuitamente na documentação oficial do NVD:

https://nvd.nist.gov/developers/request-an-api-key

No Windows PowerShell:

```powershell
$env:NVD_API_KEY="sua-chave"
```

Linux/macOS:

```bash
export NVD_API_KEY="sua-chave"
```

---

# Executando o projeto

## 1. Banco de dados

A configuração atual do backend utiliza:

```text
Host: localhost
Porta: 3306
Banco: sentry_db
Usuário: root
Senha: 123456
```

Um exemplo para executar o MySQL através do Docker:

```bash
docker run --name sentry_db \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=sentry_db \
  -p 3306:3306 \
  -d mysql:9.7
```

---

## 2. RabbitMQ

Execute:

```bash
docker run -d \
  --name sentry-rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

Painel administrativo:

```text
http://localhost:15672
```

Credenciais padrão para desenvolvimento:

```text
Usuário: guest
Senha: guest
```

A aplicação utiliza:

```text
localhost:5672
```

para comunicação com o RabbitMQ.

---

## 3. Backend

Entre na pasta:

```bash
cd backend
```

Execute:

### Linux/macOS

```bash
./mvnw spring-boot:run
```

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

Por padrão, a API estará disponível em:

```text
http://localhost:8080
```

---

## 4. Frontend

Entre na pasta:

```bash
cd frontend
```

Instale as dependências:

```bash
npm install
```

Configure a URL da API no Windows PowerShell:

```powershell
$env:VITE_SENTRY_API="http://localhost:8080"
```

Execute:

```bash
npm run dev
```

O Vite disponibilizará o frontend normalmente em:

```text
http://localhost:5173
```

---

# Laboratório de testes

O projeto possui um ambiente Docker destinado a fornecer máquinas/serviços controlados para testar as funcionalidades de descoberta do Sentry.

Para iniciar o laboratório:

```bash
docker compose -f lab-hosts/lab-docker-compose.yml up -d
```

Verifique os containers:

```bash
docker ps
```

Após iniciar o laboratório, os alvos disponíveis podem ser utilizados nas solicitações de scan.

Exemplo:

```json
{
  "targets": "127.0.0.1"
}
```

Ao finalizar os testes, derrube o laboratório:

```bash
docker compose -f lab-hosts/lab-docker-compose.yml down
```

> [!WARNING]
> O laboratório deve ser utilizado apenas para testes controlados. Nunca execute varreduras contra sistemas de terceiros sem autorização.

---

# API

A aplicação disponibiliza documentação através do **OpenAPI / Swagger UI**.

Com o backend em execução:

```text
http://localhost:8080/swagger-ui/index.html
```

Especificação OpenAPI:

```text
http://localhost:8080/v3/api-docs
```

## Principais recursos

A API está organizada em recursos relacionados a:

- autenticação;
- usuários;
- scans;
- grupos de execução;
- ativos;
- serviços;
- findings;
- políticas;
- regras de políticas.

### Solicitar uma varredura

```http
POST /scan/exec
Content-Type: application/json
Authorization: Bearer <token>
```

Exemplo:

```json
{
  "targets": "192.168.1.10,192.168.1.20"
}
```

Cada alvo é validado e processado de forma independente através do RabbitMQ.

---

# Segurança

O Sentry utiliza **Spring Security** para proteção da API e **JWT** para autenticação.

O fluxo básico é:

```text
Login
  │
  ▼
AuthService
  │
  ▼
JWT
  │
  ▼
Authorization: Bearer <token>
  │
  ▼
Spring Security
  │
  ├── autenticado
  └── não autorizado
```

Os endpoints administrativos podem ser protegidos de acordo com a role do usuário.

A aplicação também possui tratamento específico para respostas `401 Unauthorized` e `403 Forbidden`.

---

# Tratamento de falhas

O processamento das varreduras considera diferentes tipos de falha.

### Falhas transitórias

São erros que podem ser resolvidos através de uma nova tentativa, como determinadas falhas de comunicação ou indisponibilidade temporária.

Esses erros podem ser processados através da política de retry configurada no RabbitMQ.

### Falhas permanentes

São erros que não devem ser resolvidos simplesmente repetindo a mesma operação, como determinados erros de execução ou validação.

Esses erros são tratados separadamente para evitar ciclos de reprocessamento indefinidos.

### Timeout do Nmap

Cada execução do Nmap possui um limite de tempo configurável:

```text
NMAP_TIMEOUT_SECONDS=120
```

Isso impede que uma varredura bloqueada mantenha indefinidamente um processo ativo no backend.

---

# Findings

O modelo de `Finding` centraliza diferentes tipos de problemas encontrados durante uma varredura.

Um finding pode representar, por exemplo:

```text
VULNERABILITY
    └── CVE associada a um serviço descoberto

POLICY_VIOLATION
    └── Serviço ou configuração que viola uma regra
```

Os findings possuem ciclo de vida próprio e podem ser:

- abertos;
- tratados;
- resolvidos;
- reavaliados.

A identificação por fingerprint permite reconciliar resultados de diferentes scans e evitar duplicações desnecessárias.

---

# Desenvolvimento

Para informações específicas de desenvolvimento, comandos auxiliares, laboratório e execução local, consulte:

`dev-README.md`

Durante o desenvolvimento, recomenda-se manter as seguintes práticas:

- não versionar segredos;
- utilizar variáveis de ambiente;
- executar os testes antes de realizar alterações estruturais;
- utilizar o laboratório controlado para testes de Nmap;
- validar as mudanças no fluxo completo de scan;
- manter a documentação da API atualizada.

---

# Testes

Os testes automatizados estão localizados em:

```text
backend/src/test/
```

Atualmente existem testes relacionados a componentes como:

- inicialização da aplicação;
- propriedades de segurança;
- autenticação;
- gerenciamento de usuários;
- bootstrap do administrador;
- ciclo de vida de findings;
- reconciliação de findings;
- persistência/atualização de ativos.

A estratégia de testes continuará sendo expandida para cobrir principalmente:

- execução e parsing do Nmap;
- validação de alvos;
- processamento RabbitMQ;
- consultas ao NVD;
- avaliação de políticas;
- fluxo completo de uma varredura.

---

# Status do projeto

O Sentry encontra-se em fase avançada de desenvolvimento.

### Implementado

- [x] Backend Spring Boot
- [x] API REST
- [x] Execução de Nmap
- [x] Parsing do resultado XML
- [x] Descoberta de hosts, portas e serviços
- [x] Identificação de CPE
- [x] Consulta ao NVD
- [x] RabbitMQ
- [x] Processamento assíncrono
- [x] Retry e tratamento de falhas
- [x] Persistência com JPA/Hibernate
- [x] MySQL
- [x] Assets e Services
- [x] Scan e ScanExecutionGroup
- [x] Findings
- [x] Reconciliação de Findings
- [x] Policies e Policy Rules
- [x] Verificações de configuração
- [x] Autenticação JWT
- [x] Controle de acesso
- [x] Gerenciamento de usuários
- [x] Frontend React
- [x] Rotas protegidas
- [x] Swagger / OpenAPI
- [x] Ambiente de laboratório Docker

### Em desenvolvimento

- [ ] Completar integração de todas as telas com a API
- [ ] Dashboard com indicadores consolidados
- [ ] Ampliar cobertura de testes automatizados
- [ ] Testes de integração do fluxo completo de scan
- [ ] Docker Compose completo da aplicação
- [ ] Refinamento da interface
- [ ] Validação e documentação dos experimentos do TCC

---

# Contexto acadêmico

O Sentry é desenvolvido como projeto de **Trabalho de Conclusão de Curso**, explorando a aplicação de conceitos de desenvolvimento de software e segurança da informação.

Entre os principais conceitos estudados e aplicados estão:

- descoberta de ativos;
- análise de serviços;
- identificação de vulnerabilidades;
- CVE e CPE;
- NVD;
- políticas de segurança;
- mensageria assíncrona;
- processamento concorrente;
- persistência de dados;
- autenticação e autorização;
- gerenciamento do ciclo de vida de achados.

O projeto busca demonstrar não apenas a execução de ferramentas de segurança, mas a construção de uma plataforma capaz de **orquestrar, persistir, correlacionar e organizar os resultados dessas análises**.

---

## Autores

Projeto desenvolvido por:

- Gustavo Santos
- Leandro Ribeiro
- Sophia Trotte
- Vivian Mota

---

## Licença

Projeto desenvolvido para fins acadêmicos.