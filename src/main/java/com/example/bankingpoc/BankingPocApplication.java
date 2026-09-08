package com.example.bankingpoc;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Deployment(resources = "classpath*:/processes/**")
public class BankingPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingPocApplication.class, args);
    }
}
