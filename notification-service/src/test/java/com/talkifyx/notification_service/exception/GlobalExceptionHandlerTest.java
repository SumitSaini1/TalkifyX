package com.talkifyx.notification_service.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound_Returns404WithMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Notification not found");
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Notification not found", response.getBody().get("message"));
    }

    @Test
    void handleBadRequest_Returns400WithMessage() {
        BadRequestException ex = new BadRequestException("Bad input");
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad input", response.getBody().get("message"));
    }

    @Test
    void handleValidation_Returns400WithFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "recipientId", "must not be null");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("recipientId"));
    }

    @Test
    void handleValidation_NoFieldErrors_ReturnsFallbackMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals("Validation failed", response.getBody().get("message"));
    }

    @Test
    void handleRuntime_Returns400() {
        RuntimeException ex = new RuntimeException("runtime error");
        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("runtime error", response.getBody().get("message"));
    }

    @Test
    void handleGeneral_Returns500() {
        Exception ex = new Exception("unexpected error");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("unexpected error"));
    }

    @Test
    void responseBody_ContainsTimestampAndErrorFields() {
        ResourceNotFoundException ex = new ResourceNotFoundException("test");
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);

        assertNotNull(response.getBody().get("timestamp"));
        assertNotNull(response.getBody().get("error"));
        assertEquals("Not Found", response.getBody().get("error"));
    }
}