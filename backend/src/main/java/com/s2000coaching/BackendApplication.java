package com.s2000coaching;

import com.s2000coaching.config.AppTimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(BackendApplication.class);
		// Once the configuration is read and before any bean exists, so nothing is built on the
		// host's zone and then has to be corrected.
		application.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event ->
				AppTimeZone.applyToJvm(event.getEnvironment()));
		application.run(args);
	}

}
