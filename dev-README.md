## Comando para executar o Spring Boot
```bash
./mvnw spring-boot:run
```

## Comando docker para levantar um RabbitMQ
```
docker run -d --name sentry-rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```
- http://localhost:15672/   (painel adm)
- http://localhost:5672/    (app acessa)
- Senha e user padrao: guest (desenvolvimento)

## Comando para executar o React

```
$env:VITE_SENTRY_API="http://localhost:8080"        
npm run dev
```

## Lab de testes do Scan

- Subir imagem: (primeira vez demora um pouco)
    ```bash
    docker compose -f lab-hosts/lab1-compose.yml up -d
    docker compose -f lab-hosts/lab2-compose.yml up -d
    ```
- Confirmar que subiu: 
    ```bash
    docker ps
    ```
- Descobrir IP interno do lab: 
    ```bash
    docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' sentry-target1
    docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' sentry-target2
    ```
- apontar scan para: `{ "targets": "IP_DESCOBERTO" }`
- SEMPRE DERRUBAR APÓS O TÉRMINO DOS TESTES = `docker compose -f lab-hosts/lab-docker-compose.yml down`

## Comando para levantar o banco de dados
   ```bash
    docker run --name sentry_db -e MYSQL_ROOT_PASSWORD=123456 -e MYSQL_DATABASE=sentry_db -p 3306:3306 -d mysql:9.7
   ```


