package br.uva.tcc.sentry.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SCAN_QUEUE = "Scanning_Queue";

    @Bean
    public Queue minhaFila() {
        return new Queue(SCAN_QUEUE, true);
    }
}
