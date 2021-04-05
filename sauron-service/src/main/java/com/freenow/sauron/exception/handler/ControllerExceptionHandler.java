package com.freenow.sauron.exception.handler;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class ControllerExceptionHandler
{
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, Object>> handleBindingErrors(final MethodArgumentNotValidException ex, final HttpServletRequest request)
    {
        final String requestUri = request.getRequestURI();
        log.error("Validation failed for payload: {}. URI: {}.", ex.getBindingResult().getTarget(), requestUri, ex);

        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", new Date());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", "");
        body.put("path", requestUri);

        return ResponseEntity.badRequest().body(body);
    }
}
