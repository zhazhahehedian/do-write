package com.dpbug.server;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Do-Write AI 写作助手平台 - 主启动类
 *
 * @author dpbug
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.dpbug")
@MapperScan("com.dpbug.server.mapper")
public class DoWriteApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext application = SpringApplication.run(DoWriteApplication.class, args);

        Environment env = application.getEnvironment();
        String port = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String profile = env.getProperty("spring.profiles.active", "default");

        log.info("\n----------------------------------------------------------\n\t" +
                "Application '{}' is running!\n\t" +
                "Local: \t\thttp://localhost:{}{}\n\t" +
                "Profile: \t{}\n" +
                "----------------------------------------------------------",
                env.getProperty("spring.application.name"),
                port,
                contextPath,
                profile
        );
    }
}