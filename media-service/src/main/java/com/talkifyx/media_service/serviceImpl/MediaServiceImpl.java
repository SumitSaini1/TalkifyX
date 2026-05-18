package com.talkifyx.media_service.serviceImpl;

import com.talkifyx.media_service.entity.MediaFile;
import com.talkifyx.media_service.repository.MediaRepository;
import com.talkifyx.media_service.service.MediaService;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MediaRepository mediaRepository;

    @Value("${aws.bucketName}")
    private String bucket;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/zip");
    private static final List<String> IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_SIZE_BYTES = 25L * 1024 * 1024;

    @Override
    public MediaFile uploadFile(MultipartFile file, Long roomId, String messageId) {
        validate(file);
        String key = "files/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        uploadToS3(file, key);
        String presignedUrl = generatePresignedUrl(key);

        String thumbUrl = null;
        String thumbKey = null;
        if (IMAGE_TYPES.contains(file.getContentType())) {
            thumbKey = "thumbs/thumb_" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            thumbUrl = generateThumbnailAndUpload(file, thumbKey);
        }

        MediaFile media = MediaFile.builder()
                .filename(key)
                .originalName(file.getOriginalFilename())
                .url(presignedUrl)
                .thumbnailUrl(thumbUrl)
                .mimeType(file.getContentType())
                .sizeKb(file.getSize() / 1024)
                .roomId(roomId)
                .messageId(messageId)
                .build();
        return mediaRepository.save(media);
    }

    @Override
    public MediaFile uploadImage(MultipartFile file, Long roomId, String messageId) {
        if (!IMAGE_TYPES.contains(file.getContentType()))
            throw new IllegalArgumentException("Not an image type");
        return uploadFile(file, roomId, messageId);
    }

    @Override
    public Optional<MediaFile> getFileById(String mediaId) {
        return mediaRepository.findByMediaId(mediaId).map(this::refreshUrl);
    }

    @Override
    public List<MediaFile> getFilesByRoom(Long roomId) {
        return mediaRepository.findByRoomId(roomId).stream().map(this::refreshUrl).toList();
    }

    @Override
    public List<MediaFile> getFilesByUploader(Long uploaderId) {
        return mediaRepository.findByUploaderId(uploaderId).stream().map(this::refreshUrl).toList();
    }

   

    @Override
    public String generateThumbnail(String s3Key) {
        return generatePresignedUrl(s3Key);
    }

    @Override
    public List<MediaFile> getAllFiles() {
        return mediaRepository.findAll().stream().map(this::refreshUrl).toList();
    }

    @Override
    public int getFileCount(Long roomId) {
        return mediaRepository.countByRoomId(roomId);
    }

    // ── helpers ──────────────────────────────────────────────

    private void validate(MultipartFile file) {
        if (file.getSize() > MAX_SIZE_BYTES)
            throw new IllegalArgumentException("File exceeds 25MB limit");
        if (!ALLOWED_TYPES.contains(file.getContentType()))
            throw new IllegalArgumentException("File type not allowed: " + file.getContentType());
    }

    private void uploadToS3(MultipartFile file, String key) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucket).key(key).contentType(file.getContentType()).build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (Exception e) {
            throw new RuntimeException("S3 upload failed: " + e.getMessage());
        }
    }

    private String generatePresignedUrl(String key) {
        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(24))
                .getObjectRequest(r -> r.bucket(bucket).key(key))
                .build();
        return s3Presigner.presignGetObject(req).url().toString();
    }

    private String generateThumbnailAndUpload(MultipartFile file, String thumbKey) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(file.getInputStream()).size(200, 200).toOutputStream(out);
            byte[] bytes = out.toByteArray();
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucket).key(thumbKey).contentType(file.getContentType()).build(),
                    RequestBody.fromInputStream(new ByteArrayInputStream(bytes), bytes.length));
            return generatePresignedUrl(thumbKey);
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteFromS3(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception ignored) {
        }
    }

    @Override
    @Transactional
    public void deleteFile(String mediaId) {
        mediaRepository.findByMediaId(mediaId).ifPresent(f -> {
            deleteFromS3(f.getFilename());
            if (f.getThumbnailUrl() != null)
                deleteFromS3("thumbs/thumb_" + extractKey(f.getThumbnailUrl()));
            mediaRepository.deleteByMediaId(mediaId);
        });
    }

    private MediaFile refreshUrl(MediaFile f) {
        f.setUrl(generatePresignedUrl(f.getFilename()));
        return f;
    }

    private String extractKey(String url) {
        return url.substring(url.lastIndexOf('/') + 1).split("\\?")[0];
    }

}