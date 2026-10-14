package org.example.achat;

import org.springframework.boot.SpringApplication;

public class TestAchatApplication {

    public static void main(String[] args) {
        SpringApplication.from(AchatApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
