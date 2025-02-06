package com.example.securityExam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SecurityExamApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecurityExamApplication.class, args);
	}

}
