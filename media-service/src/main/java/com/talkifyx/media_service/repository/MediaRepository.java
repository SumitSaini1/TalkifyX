package com.talkifyx.media_service.repository;

import com.talkifyx.media_service.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<MediaFile, String> {
    List<MediaFile> findByUploaderId(Long uploaderId);
    List<MediaFile> findByRoomId(Long roomId);
    List<MediaFile> findByMessageId(String messageId);
    Optional<MediaFile> findByMediaId(String mediaId);
    List<MediaFile> findByMimeType(String mimeType);
    int countByRoomId(Long roomId);
    void deleteByMediaId(String mediaId);
}