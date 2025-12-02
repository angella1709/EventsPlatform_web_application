package com.example.angella.eventsplatform.web.advice;

import com.example.angella.eventsplatform.exception.AccessDeniedException;
import com.example.angella.eventsplatform.exception.EntityNotFoundException;
import com.example.angella.eventsplatform.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class CustomExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(EntityNotFoundException ex,
                                                    HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());

        if (isApiRequest(request)) {
            // JSON для API клиентов
            ErrorResponse error = ErrorResponse.builder()
                    .code("RESOURCE_NOT_FOUND")
                    .message(ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } else {
            // HTML для браузера - используем Thymeleaf шаблон
            ModelAndView mav = new ModelAndView("error/404");
            mav.addObject("errorMessage", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mav);
        }
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAuthorizationFailure(AccessDeniedException ex,
                                                        HttpServletRequest request) {
        log.info("Access denied: {}", ex.getMessage());

        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.builder()
                    .code("ACCESS_DENIED")
                    .message(ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } else {
            ModelAndView mav = new ModelAndView("error/403");
            mav.addObject("errorMessage", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mav);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        String errorDetails = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errorDetails);

        // Валидационные ошибки обычно только для API
        ErrorResponse error = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Validation failed: " + errorDetails)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleSystemError(Exception ex, HttpServletRequest request) {
        String errorId = "ERR-" + System.currentTimeMillis();
        log.error("System error [{}]: ", errorId, ex);

        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.builder()
                    .code("INTERNAL_ERROR")
                    .message("Error reference: " + errorId)
                    .timestamp(LocalDateTime.now())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } else {
            ModelAndView mav = new ModelAndView("error/500");
            mav.addObject("errorId", errorId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mav);
        }
    }

    // Определяет, является ли запрос API запросом или запросом страницы
    private boolean isApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");
        String contentType = request.getHeader("Content-Type");

        // Если путь начинается с /api/ - это API
        if (path.startsWith("/api/")) {
            return true;
        }

        // Если в Accept header есть application/json - это API
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            return true;
        }

        // Если Content-Type - application/json - это API
        if (contentType != null && contentType.contains("application/json")) {
            return true;
        }

        // Если это AJAX запрос
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return true;
        }

        // Если запрос приходит от Postman или других API клиентов
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && (userAgent.contains("Postman") || userAgent.contains("curl"))) {
            return true;
        }

        // По умолчанию считаем, что это запрос страницы
        return false;
    }
}