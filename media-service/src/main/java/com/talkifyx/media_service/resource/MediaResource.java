package com.talkifyx.media_service.resource;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.talkifyx.media_service.entity.MediaFile;
import com.talkifyx.media_service.service.MediaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Media File Operations")
public class MediaResource {

    private final MediaService mediaService;

    @PostMapping("/upload")
    @Operation(summary = "Upload any allowed file")
    public ResponseEntity<MediaFile> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long roomId,
            @RequestParam(required = false) String messageId,
            @RequestHeader("X-User-Id") Long userId) {
        MediaFile saved = mediaService.uploadFile(file, roomId, messageId);
        saved.setUploaderId(userId);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/upload/image")
    @Operation(summary = "Upload image with thumbnail generation")
    public ResponseEntity<MediaFile> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long roomId,
            @RequestParam(required = false) String messageId,
            @RequestHeader("X-User-Id") Long userId) {
        MediaFile saved = mediaService.uploadImage(file, roomId, messageId);
        saved.setUploaderId(userId);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{mediaId}")
    @Operation(summary = "Get file by ID (fresh pre-signed URL)")
    public ResponseEntity<MediaFile> getById(@PathVariable String mediaId) {
        return mediaService.getFileById(mediaId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/room/{roomId}")
    @Operation(summary = "Get all files in a room")
    public ResponseEntity<List<MediaFile>> getByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(mediaService.getFilesByRoom(roomId));
    }

    @GetMapping("/uploader/{uploaderId}")
    @Operation(summary = "Get all files by uploader")
    public ResponseEntity<List<MediaFile>> getByUploader(@PathVariable Long uploaderId) {
        return ResponseEntity.ok(mediaService.getFilesByUploader(uploaderId));
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete a file")
    public ResponseEntity<Void> delete(@PathVariable String mediaId) {
        mediaService.deleteFile(mediaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    @Operation(summary = "Get all files")
    public ResponseEntity<List<MediaFile>> getAll() {
        return ResponseEntity.ok(mediaService.getAllFiles());
    }

    @GetMapping("/count/{roomId}")
    @Operation(summary = "Get file count for a room")
    public ResponseEntity<Integer> getCount(@PathVariable Long roomId) {
        return ResponseEntity.ok(mediaService.getFileCount(roomId));
    }
} 
    

