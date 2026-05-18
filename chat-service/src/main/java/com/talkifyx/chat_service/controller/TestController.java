package com.talkifyx.chat_service.controller;

import com.talkifyx.chat_service.client.RoomServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TestController {

    @Autowired
    private RoomServiceClient roomServiceClient;

    @GetMapping("/test/room/{id}/members")
    public List<Map<String, Object>> testMembers(@PathVariable Long id) {
        return roomServiceClient.getRoomMembers(id);
    }
}
