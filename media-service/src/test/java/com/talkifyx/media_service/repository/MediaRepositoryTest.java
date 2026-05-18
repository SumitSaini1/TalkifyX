package com.talkifyx.media_service.repository;

import com.talkifyx.media_service.entity.MediaFile;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MediaRepositoryTest {

    @Autowired MediaRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private MediaFile save(Long uploaderId, Long roomId, String messageId, String mime) {
        return repository.save(MediaFile.builder()
                .uploaderId(uploaderId)
                .roomId(roomId)
                .messageId(messageId)
                .filename("files/uuid_file.jpg")
                .originalName("file.jpg")
                .url("https://s3.example.com/file.jpg")
                .mimeType(mime)
                .sizeKb(100L)
                .build());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  findByMediaId
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByMediaId()")
    class FindByMediaId {

        @Test
        @DisplayName("returns present Optional for saved media")
        void found() {
            MediaFile saved = save(1L, 10L, "msg-1", "image/jpeg");

            Optional<MediaFile> result = repository.findByMediaId(saved.getMediaId());

            assertThat(result).isPresent();
            assertThat(result.get().getMediaId()).isEqualTo(saved.getMediaId());
        }

        @Test
        @DisplayName("returns empty for unknown mediaId")
        void notFound() {
            assertThat(repository.findByMediaId("nonexistent-uuid")).isEmpty();
        }

        @Test
        @DisplayName("prePersist sets mediaId and uploadedAt automatically")
        void prePersistSetsFields() {
            MediaFile saved = save(1L, 10L, "msg-1", "image/jpeg");

            assertThat(saved.getMediaId()).isNotNull().isNotEmpty();
            assertThat(saved.getUploadedAt()).isNotNull();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  findByUploaderId
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByUploaderId()")
    class FindByUploaderId {

        @Test
        @DisplayName("returns all files for uploader")
        void found() {
            save(5L, 10L, "m1", "image/jpeg");
            save(5L, 11L, "m2", "image/png");
            save(6L, 10L, "m3", "application/pdf"); // different uploader

            List<MediaFile> result = repository.findByUploaderId(5L);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(f -> f.getUploaderId().equals(5L));
        }

        @Test
        @DisplayName("returns empty for unknown uploader")
        void notFound() {
            assertThat(repository.findByUploaderId(999L)).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  findByRoomId
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByRoomId()")
    class FindByRoomId {

        @Test
        @DisplayName("returns all files for room")
        void found() {
            save(1L, 20L, "m1", "image/jpeg");
            save(2L, 20L, "m2", "image/png");
            save(3L, 30L, "m3", "application/pdf"); // different room

            List<MediaFile> result = repository.findByRoomId(20L);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(f -> f.getRoomId().equals(20L));
        }

        @Test
        @DisplayName("returns empty for unknown room")
        void empty() {
            assertThat(repository.findByRoomId(888L)).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  findByMessageId
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByMessageId()")
    class FindByMessageId {

        @Test
        @DisplayName("returns files linked to a message")
        void found() {
            save(1L, 10L, "msg-abc", "image/jpeg");
            save(2L, 10L, "msg-abc", "image/png");
            save(3L, 10L, "msg-xyz", "image/gif");

            List<MediaFile> result = repository.findByMessageId("msg-abc");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns empty for unknown messageId")
        void empty() {
            assertThat(repository.findByMessageId("no-such-msg")).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  findByMimeType
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByMimeType()")
    class FindByMimeType {

        @Test
        @DisplayName("returns only files matching mime type")
        void found() {
            save(1L, 10L, "m1", "image/jpeg");
            save(2L, 10L, "m2", "image/jpeg");
            save(3L, 10L, "m3", "application/pdf");

            List<MediaFile> result = repository.findByMimeType("image/jpeg");

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(f -> f.getMimeType().equals("image/jpeg"));
        }

        @Test
        @DisplayName("returns empty when no files match mime type")
        void empty() {
            save(1L, 10L, "m1", "image/jpeg");

            assertThat(repository.findByMimeType("video/mp4")).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  countByRoomId
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("countByRoomId()")
    class CountByRoomId {

        @Test
        @DisplayName("returns correct count for room")
        void correctCount() {
            save(1L, 50L, "m1", "image/jpeg");
            save(2L, 50L, "m2", "image/png");
            save(3L, 50L, "m3", "application/pdf");
            save(4L, 60L, "m4", "image/gif"); // different room

            assertThat(repository.countByRoomId(50L)).isEqualTo(3);
        }

        @Test
        @DisplayName("returns 0 for room with no files")
        void zero() {
            assertThat(repository.countByRoomId(999L)).isZero();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  deleteByMediaId
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteByMediaId()")
    class DeleteByMediaId {

        @Test
        @DisplayName("removes the record from database")
        void deletesRecord() {
            MediaFile saved = save(1L, 10L, "m1", "image/jpeg");
            String id = saved.getMediaId();

            repository.deleteByMediaId(id);

            assertThat(repository.findByMediaId(id)).isEmpty();
        }

        @Test
        @DisplayName("does not affect other records")
        void doesNotAffectOthers() {
            MediaFile a = save(1L, 10L, "m1", "image/jpeg");
            MediaFile b = save(2L, 10L, "m2", "image/png");

            repository.deleteByMediaId(a.getMediaId());

            assertThat(repository.findByMediaId(b.getMediaId())).isPresent();
        }

        @Test
        @DisplayName("no error when mediaId does not exist")
        void noErrorOnMissing() {
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> repository.deleteByMediaId("ghost-id")
            );
        }
    }
}
