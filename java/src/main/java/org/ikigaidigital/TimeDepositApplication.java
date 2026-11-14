package org.ikigaidigital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Composition root: starts Spring Boot and wires the legacy calculator as a bean.
 */
@SpringBootApplication
public class TimeDepositApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimeDepositApplication.class, args);
    }

    @Bean
    TimeDepositCalculator timeDepositCalculator() {
        return new TimeDepositCalculator();
    }
}
