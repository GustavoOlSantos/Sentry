package br.uva.tcc.sentry;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import br.uva.tcc.sentry.config.AppTime;

@SpringBootApplication
@EnableRabbit
@EnableCaching
public class SentryApplication {

	public static void main(String[] args) {
		AppTime.pinStorageZone(); // horários gravados sempre em UTC, independente do servidor
		SpringApplication.run(SentryApplication.class, args);
	}

}
