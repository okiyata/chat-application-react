package com.alibou.websocket.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Response {
    private HttpStatus status;
    private Integer statusCode;
    private String message;
    private Map<String, Object> responseList;

    public static class ResponseBuilder {
        public ResponseBuilder response(String key, Object value) {
            if (responseList == null) responseList = new HashMap<>();
            responseList.put(key, value);
            return this;
        }

        public ResponseEntity<Response> buildEntity() {
            if (status == null) status = HttpStatus.OK;
            return new ResponseEntity<>(new Response(status, status.value(), message, responseList), status);
        }

        public ResponseEntity<Response> buildEntity(HttpHeaders headers) {
            if (status == null) status = HttpStatus.OK;
            return new ResponseEntity<>(new Response(status, status.value(), message, responseList), headers, status);
        }
    }
}
