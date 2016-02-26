package com.dipcoin.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@SpringBootApplication
public class DipcoinOfflineSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DipcoinOfflineSchedulerApplication.class, args);
		new ClassPathXmlApplicationContext("recon-scheduler-config.xml");
	}
}
