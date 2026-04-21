package com.talkifyx.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ONLINE;

    private String provider;

    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;

    public enum Status {
        ONLINE, AWAY, DND, INVISIBLE
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        lastSeenAt = LocalDateTime.now();
    }
}