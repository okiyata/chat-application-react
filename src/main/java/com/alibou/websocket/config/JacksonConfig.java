package com.alibou.websocket.config;

import com.alibou.websocket.chat.TimestampDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.cloud.Timestamp;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Timestamp.class, new TimestampDeserializer());
        mapper.registerModule(module);
        return mapper;
    }
}
