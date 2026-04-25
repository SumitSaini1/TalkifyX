package com.talkifyx.media_service.service;

import com.talkifyx.media_service.entity.MediaFile;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;

public interface MediaService {
    MediaFile uploadFile(MultipartFile file, Long roomId, String messageId);
    MediaFile uploadImage(MultipartFile file, Long roomId, String messageId);
    Optional<MediaFile> getFileById(String mediaId);
    List<MediaFile> getFilesByRoom(Long roomId);
    List<MediaFile> getFilesByUploader(Long uploaderId);
    void deleteFile(String mediaId);
    String generateThumbnail(String s3Key);
    List<MediaFile> getAllFiles();
    int getFileCount(Long roomId);
}