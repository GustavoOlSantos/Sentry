
package br.uva.tcc.sentry.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String SCAN_QUEUE = "Scanning_Queue";
    public static final String SCAN_DLX = "sentry.scan.dlx";
    public static final String SCAN_DLQ = "Scanning_Queue.dlq";
    public static final String SCAN_DLQ_ROUTING_KEY = "scan.failed";

    @Bean
    public Queue scanQueue() {
        return QueueBuilder.durable(SCAN_QUEUE)
            .deadLetterExchange(SCAN_DLX)
            .deadLetterRoutingKey(SCAN_DLQ_ROUTING_KEY)
            .build();
    }

    @Bean
    public DirectExchange scanDeadLetterExchange() {
        return new DirectExchange(SCAN_DLX, true, false);
    }

    @Bean
    public Queue scanDeadLetterQueue() {
        return QueueBuilder.durable(SCAN_DLQ).build();
    }

    @Bean
    public Binding scanDeadLetterBinding() {
        return BindingBuilder.bind(scanDeadLetterQueue())
            .to(scanDeadLetterExchange())
            .with(SCAN_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter, RabbitTemplateConfigurer configurer) {
        var rabbitTemplate = new RabbitTemplate();
        configurer.configure(rabbitTemplate, connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonMessageConverter);
        return rabbitTemplate;
    }
}