package br.uva.tcc.sentry.scan.messaging;

import br.uva.tcc.sentry.config.RabbitMQConfig;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScanProducer {

    private final RabbitTemplate rabbitTemplate;

    public ScanProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enviarMensagem(ScanMessage scanMessage) {
        var correlationData = new CorrelationData(scanMessage.scanId().toString());

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.SCAN_QUEUE,
            scanMessage,
            message -> {
                var properties = message.getMessageProperties();
                properties.setMessageId(scanMessage.scanId().toString());
                properties.setCorrelationId(scanMessage.execGroupId().toString());
                properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return message;
            },
            correlationData
        );

        try {
            var confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);

            if (!confirm.ack()) {
                throw new AmqpException("RabbitMQ não confirmou a publicação do scan.");
            }

            if (correlationData.getReturned() != null) {
                throw new AmqpException("RabbitMQ não roteou a mensagem para uma fila.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AmqpException("A confirmação da publicação foi interrompida.", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new AmqpException("Não foi possível confirmar a publicação do scan.", e);
        }
    }
}
