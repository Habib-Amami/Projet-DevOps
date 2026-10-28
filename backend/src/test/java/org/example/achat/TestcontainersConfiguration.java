package org.example.achat;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        // Same version as compose.yaml and k8s/mysql.yaml. Data lives in RAM
        // (tmpfs): Docker Desktop disks make MySQL initialization very slow.
        return new MySQLContainer(DockerImageName.parse("mysql:8.4"))
                .withTmpFs(Map.of("/var/lib/mysql", "rw"))
                .withStartupTimeout(Duration.ofMinutes(5));
    }

}
