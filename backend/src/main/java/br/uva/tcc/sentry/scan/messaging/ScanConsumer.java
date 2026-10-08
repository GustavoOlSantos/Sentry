package br.uva.tcc.sentry.scan.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import br.uva.tcc.sentry.config.RabbitMQConfig;

@Component
public class ScanConsumer {
    
    @RabbitListener(queues = RabbitMQConfig.SCAN_QUEUE)
    public void consumirMensagem(String mensagem) {
        System.out.println("Mensagem recebida: " + mensagem);

        // Implemente aqui a lógica de processamento.
    }
}
