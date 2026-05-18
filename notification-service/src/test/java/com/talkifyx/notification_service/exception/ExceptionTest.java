package com.talkifyx.notification_service.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void resourceNotFoundException_MessageSetCorrectly() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        assertEquals("Not found", ex.getMessage());
    }

    @Test
    void badRequestException_MessageSetCorrectly() {
        BadRequestException ex = new BadRequestException("Bad input");
        assertEquals("Bad input", ex.getMessage());
    }

    @Test
    void resourceNotFoundException_IsRuntimeException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("test");
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void badRequestException_IsRuntimeException() {
        BadRequestException ex = new BadRequestException("test");
        assertInstanceOf(RuntimeException.class, ex);
    }
}