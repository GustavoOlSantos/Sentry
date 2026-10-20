## Comando para executar o Spring Boot
```
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
npm run dev
```

## Lab de testes do Scan

- Subir imagem: (primeira vez demora um pouco)
    ```
    docker compose up -d 
    ```
- Confirmar que subiu: 
    ```
    docker ps
    ```
- apontar scan para: `{ "targets": "127.0.0.1" }`
- SEMPRE DERRUBAR APÓS O TÉRMINO DOS TESTES = `docker compose down`

## Comando para levantar o banco de dados
   ```
    docker run --name sentry_db -e MYSQL_ROOT_PASSWORD=123456 -e MYSQL_DATABASE=sentry_db -p 3306:3306 -d mysql:8.0
   ```


