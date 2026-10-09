package br.uva.tcc.sentry.scan.messaging;

import br.uva.tcc.sentry.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScanProducer {

    private final RabbitTemplate rabbitTemplate;

    public ScanProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enviarMensagem(ScanMessage scanMessage) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.SCAN_QUEUE, scanMessage);
    }
}
