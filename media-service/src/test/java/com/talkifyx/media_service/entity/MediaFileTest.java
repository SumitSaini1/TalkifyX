package com.talkifyx.media_service.entity;

import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MediaFileTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builder sets all fields correctly")
        void builder_allFields() {
            LocalDateTime now = LocalDateTime.now();
            MediaFile m = MediaFile.builder()
                    .mediaId("uuid-123")
                    .uploaderId(1L)
                    .roomId(10L)
                    .messageId("msg-001")
                    .filename("files/uuid_photo.jpg")
                    .originalName("photo.jpg")
                    .url("https://s3.example.com/photo.jpg")
                    .thumbnailUrl("https://s3.example.com/thumb.jpg")
                    .mimeType("image/jpeg")
                    .sizeKb(512L)
                    .width(1920)
                    .height(1080)
                    .uploadedAt(now)
                    .build();

            assertThat(m.getMediaId()).isEqualTo("uuid-123");
            assertThat(m.getUploaderId()).isEqualTo(1L);
            assertThat(m.getRoomId()).isEqualTo(10L);
            assertThat(m.getMessageId()).isEqualTo("msg-001");
            assertThat(m.getFilename()).isEqualTo("files/uuid_photo.jpg");
            assertThat(m.getOriginalName()).isEqualTo("photo.jpg");
            assertThat(m.getUrl()).isEqualTo("https://s3.example.com/photo.jpg");
            assertThat(m.getThumbnailUrl()).isEqualTo("https://s3.example.com/thumb.jpg");
            assertThat(m.getMimeType()).isEqualTo("image/jpeg");
            assertThat(m.getSizeKb()).isEqualTo(512L);
            assertThat(m.getWidth()).isEqualTo(1920);
            assertThat(m.getHeight()).isEqualTo(1080);
            assertThat(m.getUploadedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("no-args constructor returns all nulls")
        void noArgsConstructor() {
            MediaFile m = new MediaFile();
            assertThat(m.getMediaId()).isNull();
            assertThat(m.getUploaderId()).isNull();
            assertThat(m.getUrl()).isNull();
        }

        @Test
        @DisplayName("setters mutate correctly")
        void setters() {
            MediaFile m = MediaFile.builder().mimeType("image/jpeg").build();
            m.setMimeType("image/png");
            m.setThumbnailUrl(null);
            m.setUploaderId(99L);

            assertThat(m.getMimeType()).isEqualTo("image/png");
            assertThat(m.getThumbnailUrl()).isNull();
            assertThat(m.getUploaderId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("partial builder — only required fields")
        void partialBuilder() {
            MediaFile m = MediaFile.builder().roomId(5L).mimeType("application/pdf").build();
            assertThat(m.getRoomId()).isEqualTo(5L);
            assertThat(m.getMimeType()).isEqualTo("application/pdf");
            assertThat(m.getMediaId()).isNull();
            assertThat(m.getThumbnailUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("prePersist()")
    class PrePersistTests {

        @Test
        @DisplayName("sets mediaId when null")
        void setsMediaId() {
            MediaFile m = new MediaFile();
            assertThat(m.getMediaId()).isNull();

            m.prePersist();

            assertThat(m.getMediaId()).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("sets uploadedAt when null")
        void setsUploadedAt() {
            MediaFile m = new MediaFile();
            assertThat(m.getUploadedAt()).isNull();

            m.prePersist();

            assertThat(m.getUploadedAt()).isNotNull();
        }

        @Test
        @DisplayName("does NOT overwrite existing mediaId")
        void doesNotOverwriteMediaId() {
            MediaFile m = MediaFile.builder().mediaId("existing-id").build();

            m.prePersist();

            assertThat(m.getMediaId()).isEqualTo("existing-id");
        }

        @Test
        @DisplayName("does NOT overwrite existing uploadedAt")
        void doesNotOverwriteUploadedAt() {
            LocalDateTime fixed = LocalDateTime.of(2024, 1, 1, 12, 0);
            MediaFile m = MediaFile.builder().uploadedAt(fixed).build();

            m.prePersist();

            assertThat(m.getUploadedAt()).isEqualTo(fixed);
        }

        @Test
        @DisplayName("generated mediaId is a valid UUID format")
        void generatedMediaIdIsUuid() {
            MediaFile m = new MediaFile();
            m.prePersist();

            // UUID format: 8-4-4-4-12 hex chars
            assertThat(m.getMediaId())
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("two calls generate same mediaId (idempotent)")
        void idempotentMediaId() {
            MediaFile m = new MediaFile();
            m.prePersist();
            String firstId = m.getMediaId();

            m.prePersist();

            assertThat(m.getMediaId()).isEqualTo(firstId);
        }
    }
}
