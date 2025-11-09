package com.hmis.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class HmisWorkflowEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(HmisWorkflowEngineApplication.class, args);
    }
}
