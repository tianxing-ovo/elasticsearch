package com.ltx;

import org.elasticsearch.client.RestClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author tianxing
 */
@SpringBootApplication
@MapperScan("com.ltx.mapper")
public class Application {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        RestClient restClient = context.getBean(RestClient.class);
        System.out.println(restClient);
    }
}
