package org.ikigaidigital;

import org.springframework.boot.SpringApplication;

public class TestTimeDepositApplication {

    public static void main(String[] args) {
        SpringApplication.from(TimeDepositApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
