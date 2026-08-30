package com.system.dlq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.system")
@EntityScan(basePackages = {"com.system.dlq.model", "com.system.common"})
@EnableJpaRepositories(basePackages = {"com.system.dlq.repository", "com.system.common"})
public class DlqAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(DlqAdminApplication.class, args);
    }
}
