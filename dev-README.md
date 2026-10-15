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