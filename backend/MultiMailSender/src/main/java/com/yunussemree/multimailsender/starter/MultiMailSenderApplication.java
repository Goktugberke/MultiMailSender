package com.yunussemree.multimailsender.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.yunussemree.multimailsender")
@EnableJpaRepositories(basePackages = "com.yunussemree.multimailsender.repository")
@EntityScan(basePackages = "com.yunussemree.multimailsender.model")
public class MultiMailSenderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiMailSenderApplication.class, args);
    }

}
