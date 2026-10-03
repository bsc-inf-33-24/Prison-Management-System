package com.pms.api;

import org.springframework.boot.SpringApplication;

public class TestPmsApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(PmsApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
