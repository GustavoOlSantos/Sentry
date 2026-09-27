package br.uva.tcc.sentry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SentryApplication {

	public static void main(String[] args) {
		SpringApplication.run(SentryApplication.class, args);
	}

}
