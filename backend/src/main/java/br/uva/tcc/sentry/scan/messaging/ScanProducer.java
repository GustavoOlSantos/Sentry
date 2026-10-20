package br.uva.tcc.sentry.scan.messaging;

import br.uva.tcc.sentry.config.RabbitMQConfig;
import br.uva.tcc.sentry.scan.exception.ScanPublishFailedException;
import br.uva.tcc.sentry.scan.exception.ScanPublishNotConfirmedException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScanProducer {

    private final RabbitTemplate rabbitTemplate;
    private final int ACK_TIMEOUT = 5;

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
            var confirm = correlationData.getFuture()
                .get(ACK_TIMEOUT, TimeUnit.SECONDS);

            if (correlationData.getReturned() != null) {
                throw new ScanPublishFailedException("Mensagem não roteada para nenhuma fila.");
            }

            if (!confirm.ack()) {
                throw new ScanPublishNotConfirmedException("RabbitMQ rejeitou a publicação (NACK).");
            }

        } 
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScanPublishNotConfirmedException("A confirmação da publicação foi interrompida.", e);
        } 
        catch (ExecutionException e) {
            throw new ScanPublishNotConfirmedException("Falha ao obter a confirmação da publicação.", e);
        } 
        catch (TimeoutException e) {
            throw new ScanPublishNotConfirmedException("Timeout aguardando confirmação do RabbitMQ.", e);
        }

    }
}
