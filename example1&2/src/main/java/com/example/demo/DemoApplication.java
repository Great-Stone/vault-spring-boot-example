package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class DemoApplication {

	@Value("${spring.datasource.username}")
	private String ds_name;

	@Value("${spring.datasource.password}")
	private String ds_pw;

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@PostConstruct
	public void readDBconfigs() {
			log.info("Reading datasource config {} - {}", ds_name, ds_pw);
	}
}
