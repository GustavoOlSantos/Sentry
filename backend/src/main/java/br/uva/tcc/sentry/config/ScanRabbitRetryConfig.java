
package br.uva.tcc.sentry.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;

import br.uva.tcc.sentry.scan.exception.PermanentScanException;
import br.uva.tcc.sentry.scan.exception.TransientScanException;

@Configuration
public class ScanRabbitRetryConfig {

    private static final Logger log = LoggerFactory.getLogger(ScanRabbitRetryConfig.class);

    @Bean
    public SimpleRabbitListenerContainerFactory scanListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory) {

        var factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);

        RetryPolicy retryPolicy = RetryPolicy.builder()
            .maxRetries(4)
            .delay(Duration.ofSeconds(1))
            .multiplier(2.0)
            .maxDelay(Duration.ofSeconds(10))
            .predicate(cause -> {
                Throwable current = cause;

                while (current != null) {
                    if (current instanceof PermanentScanException
                            || current instanceof IllegalArgumentException) {
                        return false;
                    }

                    if (current instanceof TransientScanException) {
                        return true;
                    }

                    current = current.getCause();
                }

                return false;
            })
            .build();

        MessageRecoverer recoverer = (Message message, Throwable cause) -> {
            var properties = message.getMessageProperties();

            log.error(
                "Mensagem encaminhada para a DLQ. messageId={}, correlationId={}, routingKey={}",
                properties.getMessageId(),
                properties.getCorrelationId(),
                properties.getReceivedRoutingKey(),
                cause
            );

            throw new AmqpRejectAndDontRequeueException(
                "Falha definitiva ou esgotamento das tentativas", cause);
        };

        var interceptor = RetryInterceptorBuilder.stateless()
            .retryPolicy(retryPolicy)
            .recoverer(recoverer)
            .build();

        factory.setAdviceChain(interceptor);
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
}
