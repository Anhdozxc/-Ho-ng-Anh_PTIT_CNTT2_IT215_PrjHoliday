package vn.edu.ptit.holidayplanner.api;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiErrorResponse {
    private ApiErrorResponse() {
    }

    public static Map<String, Object> body(HttpStatus status, String message) {
        return body(status, message, Collections.emptyMap());
    }

    public static Map<String, Object> body(HttpStatus status, String message,
                                           Map<String, String> fieldErrors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("fieldErrors", fieldErrors == null ? Collections.emptyMap() : fieldErrors);
        return body;
    }
}
