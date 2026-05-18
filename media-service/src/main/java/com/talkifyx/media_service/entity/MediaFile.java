package com.talkifyx.media_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String mediaId;

    private Long uploaderId;
    private Long roomId;
    private String messageId;
    private String filename;
    private String originalName;
    @Column(columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(columnDefinition = "TEXT")
    private String url;
    private String mimeType;
    private Long sizeKb;
    private Integer width;
    private Integer height;
    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        if (mediaId == null)
            mediaId = UUID.randomUUID().toString();
        if (uploadedAt == null)
            uploadedAt = LocalDateTime.now();
    }
}