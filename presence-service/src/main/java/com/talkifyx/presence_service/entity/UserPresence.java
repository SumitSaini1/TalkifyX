package com.talkifyx.presence_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_presence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long presenceId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String status;

    private String customMessage;
    private String deviceType;
    private String ipAddress;
    private String sessionId;
    private LocalDateTime connectedAt;
    private LocalDateTime lastPingAt;
}