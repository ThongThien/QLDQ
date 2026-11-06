package com.qldq.qldqapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QldqapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(QldqapiApplication.class, args);
	}

}
