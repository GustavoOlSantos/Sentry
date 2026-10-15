## Comando para executar o Spring Boot
```
./mvnw spring-boot:run
```

## Comando docker para levantar um RabbitMQ
```
docker run -d --name meu-rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```
- http://localhost:15672/   (painel adm)
- http://localhost:5672/    (app acessa)
- Senha e user padrao: guest (desenvolvimento)

## Lab de testes do Scan
```
- Subir imagem: docker compose up -d (primeira vez demora um pouco)
- Confirmar que subiu: docker ps
- apontar scan para : { "targets": "127.0.0.1" }
- SEMPRE DERRUBAR APÓS O TÉRMINO DOS TESTES = docker compose down

-

## Comando para executar o React (ainda não tem)
```
npm run dev
```