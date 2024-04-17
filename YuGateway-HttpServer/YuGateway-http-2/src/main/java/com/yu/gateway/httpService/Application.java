package com.yu.gateway.httpService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author yu
 * @description 启动类
 * @date 2024-04-17
 */
@SpringBootApplication(scanBasePackages = "com.yu.gateway")
public class Application {


	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}