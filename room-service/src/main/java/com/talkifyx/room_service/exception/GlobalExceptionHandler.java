package com.talkifyx.room_service.exception;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RoomException.class)
    public ResponseEntity<Map<String, Object>> handleRoomException(RoomException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400, "message", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(500).body(Map.of(
                "status", 500, "message", "Internal server error"));
    }
}