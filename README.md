
# SENTRY
Plataforma de varredura de ativos, identificação de vulnerabilidades e verificações de segurança

<div align="center">
  <img src="https://skillicons.dev/icons?i=react,java,spring,rabbitmq,javascript,html,css,mysql" />
  <img height="40px" src="https://upload.wikimedia.org/wikipedia/commons/7/73/Logo_nmap.png?utm_source=commons.wikimedia.org&utm_campaign=index&utm_content=original" />
</div>

---

## 1. SOBRE O PROJETO

O Sentry é um projeto acadêmico em desenvolvimento, como TCC, criado com o objetivo de apoiar a identificação e a organização de riscos de segurança
em ambientes de rede controlados e autorizados.

A proposta é reunir, em uma única plataforma, o processo de descoberta de hosts, serviços, correlação de versões identificadas com vulnerabilidades conhecidas,
violações de políticas e configurações internas e a gestão de todos os achados em formato de cards estilo Kanban, para acompanhar e monitorar a resolução 
dos problemas encontrados.

Para o Scanner, é utilizado o Nmap para a descoberta técnica. O valor do Sentry está na orquestração do fluxo, no
tratamento dos resultados, na consulta ao catálogo de vulnerabilidades e na
organização dos achados.

>[!Warning]
> o projeto está em desenvolvimento.

## 2. OBJETIVOS

- Executar varreduras de um ou mais alvos autorizados.
- Processar cada alvo de forma assíncrona utilizando RabbitMQ.
- Identificar hosts, portas abertas, serviços, versões e informações CPE
  fornecidas pelo Nmap.
- Consultar o National Vulnerability Database (NVD) para buscar CVEs associadas
  aos CPEs identificados.
- Executar verificações preliminares de configuração insegura.
- Normalizar vulnerabilidades e violações de configuração em objetos Finding.
- Evoluir para o acompanhamento de solicitações agrupadas, histórico de scans,
  ativos e resultados persistidos.
- Futuramente, disponibilizar uma interface para visualizar os resultados e
  apoiar a gestão de vulnerabilidades e políticas de segurança.

## 3. TECNOLOGIAS
**Backend**
- Java 21
- Spring Boot 4.1
- Spring Web MVC / REST
- Spring AMQP e RabbitMQ
- Spring RestClient para integração HTTP com o NVD
- Spring Cache
- Springdoc OpenAPI / Swagger UI
- Maven Wrapper
- Lombok

**Frontend**
- React 19.2
- Vite 8.3

**Ferramentas e integrações**
- Nmap: descoberta de hosts, portas, serviços, versões e sistema operacional.
- NVD API 2.0: consulta de vulnerabilidades conhecidas por CPE.
- RabbitMQ: fila de processamento assíncrono.
- Docker: alternativa prática para executar o RabbitMQ localmente e posteriormente o compose da aplicação e máquinas vulneráveis de exemplo


## 4. COMO O SISTEMA FUNCIONA

Fluxo geral atual:
```
  Cliente HTTP
      |
      v
  POST /scan/exec
      |
      v
  ScanService
      |
      +--> separa e remove alvos repetidos
      +--> cria um identificador para cada execução
      +--> publica uma mensagem por alvo no RabbitMQ
                   |
                   v
              RabbitMQ
                   |
                   v
              ScanConsumer
                   |
                   v
              NmapScanEngine
                   |
                   +--> NmapCommandBuilder
                   +--> NmapProcessExecutor
                   +--> NmapResultParser
                   |
                   v
              Hosts e serviços descobertos
                   |
                   +--> Verificações de configuração
                   +--> Consulta ao NVD por CPE
                   +--> Criação de Findings
```
A execução é assíncrona: a API solicita o processamento e retorna sem esperar
que todas as varreduras terminem. O estado e os resultados produzidos pelo
consumidor ainda não são armazenados de forma persistente.

O projeto também está evoluindo para agrupar várias execuções em uma entidade
ScanExecutionGroup. A associação entre grupo, scans individuais e o cálculo do
estado final do grupo devem ser concluídos junto com o fluxo de persistência.

## 6. ESTRUTURA DO REPOSITÓRIO
```
sentry/
├── backend/
│    ├── src/main/java/br/uva/tcc/sentry/
│    │   ├── config/                 Configurações do Spring e do RabbitMQ
│    │   ├── Asset/domain/           Modelos de ativos e serviços
│    │   ├── scan/
│    │   │   ├── controller/           Endpoints REST de scan
│    │   │   ├── DTO/                  Objetos de requisição e resposta
│    │   │   ├── domain/               Scan, ScanResult e modelos relacionados
│    │   │   ├── messaging/             Produtor e consumidor RabbitMQ
│    │   │   ├── scanner/               Contrato do mecanismo de varredura
│    │   │   │   └── nmap/          Construção, execução e parsing do Nmap
│    │   │   └── services/              Orquestração das solicitações
│    │   ├── vulnerability/
│    │   │   ├── domain/                Modelo de vulnerabilidade
│    │   │   └── nvd/                   Cliente, mapeador e configuração do NVD
│    │   ├── finding/
│    │   │   ├── checker/               Verificações de configuração
│    │   │   ├── domain/                Modelo de Finding e enums
│    │   │   └── factory/               Criação centralizada de Findings
│    │   ├── policy/domain/            Modelos iniciais de política e regras
│    │   ├── shared/domain/            Tipos compartilhados, como Severity
│    ├── src/main/resources/
│    │   ├──  application.yaml         Configurações da aplicação
│    │   ├── logback-spring.xml       Configuração de logs
│    ├── pom.xml                    Dependências e configuração Maven
│    ├── mvnw / mvnw.cmd            Maven Wrapper
├── frontend/                    Estrutura reservada para a interface
├── README.txt                  
```

## 7. REQUISITOS PARA EXECUÇÃO LOCAL
- Java Development Kit (JDK) 21 e disponível na PATH do sistema.
- Nmap instalado e disponível no PATH do sistema.
- Node/npm instalado na máquina.
- RabbitMQ acessível pelo backend.
- Docker, caso deseje executar o RabbitMQ em um contêiner.
- No terminal, definir a variável de ambiente NVD_API_KEY (Valor na secret)
- Acesso à internet para consultar a API do NVD.
- Maven Wrapper incluído no repositório; uma instalação global do Maven não é
  necessária, desde que o Wrapper consiga baixar sua distribuição na primeira
  execução.


A execução de scans deve ser feita somente contra máquinas, redes e serviços
para os quais você possui autorização explícita.

### 8. CONFIGURAÇÃO DO AMBIENTE

#### 8.1. Iniciar RabbitMQ com Docker
Execute:

```
docker run -d --name sentry-rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

A aplicação conecta por padrão em `localhost:5672`
Painel administrativo: `http://localhost:15672/`

Credenciais padrão para desenvolvimento local:
Usuário: `guest`
Senha: `guest`

#### 8.2. Variáveis de ambiente

A variável NVD_API_KEY é opcional. Uma chave permite utilizar o limite de
requisições associado a uma chave da API do NVD; sem chave, a aplicação continua
tentando consultar a API, sujeita ao limite público.

Você pode solicitar uma chave no site oficial:
https://nvd.nist.gov/developers/request-an-api-key

Linux/macOS (exemplo):
```
export NVD_API_KEY="sua-chave"
```

Windows PowerShell (exemplo):
```
$env:NVD_API_KEY="sua-chave"
````

## 9. EXECUTAR O BACKEND

1. Abra um terminal na pasta backend:
```
cd backend
```

2. Certifique-se de que o RabbitMQ está em execução e que o comando nmap é
   reconhecido no terminal.

3. Execute com o Maven Wrapper.

Linux/macOS:
```
./mvnw spring-boot:run
```

Por padrão, o backend utiliza a porta 8080, salvo configuração externa.

Documentação OpenAPI / Swagger UI, quando a aplicação estiver iniciada:
`http://localhost:8080/swagger-ui/index.html`

Documento OpenAPI:
`http://localhost:8080/v3/api-docs`

A primeira execução do Maven Wrapper pode exigir acesso à internet para baixar
componentes de build.

## 10. ENDPOINTS REST EXISTENTES

### 10.1. Solicitar varredura

```
POST http://localhost:8080/scan/exec
Content-Type: application/json

Corpo da requisição:

{
  "targets": "192.168.1.10,192.168.1.20,servidor.exemplo.local"
}
```

O campo targets contém os alvos separados por vírgula. Cada alvo é enviado como
uma mensagem independente para processamento assíncrono.
