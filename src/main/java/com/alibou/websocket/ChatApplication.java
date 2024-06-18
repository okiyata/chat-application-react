package com.alibou.websocket;

import com.google.firebase.database.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication
@EnableWebSocket
@EnableScheduling
public class ChatApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatApplication.class, args);
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(@NotNull CorsRegistry registry) {
				registry.addMapping("/**")
						.allowedOrigins("http://localhost:3000")
						.allowedMethods("HEAD","GET","POST","PUT","DELETE","PATCH","OPTIONS")
						.allowCredentials(true);
			}
		};
	}

}
