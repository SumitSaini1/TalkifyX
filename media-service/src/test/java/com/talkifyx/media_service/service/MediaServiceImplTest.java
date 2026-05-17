package com.talkifyx.media_service.service;

import com.talkifyx.media_service.entity.MediaFile;
import com.talkifyx.media_service.repository.MediaRepository;
import com.talkifyx.media_service.serviceImpl.MediaServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock S3Client s3Client;
    @Mock S3Presigner s3Presigner;
    @Mock MediaRepository mediaRepository;
    @Mock PresignedGetObjectRequest presignedGetObjectRequest;

    @InjectMocks MediaServiceImpl service;

    // ─── helpers ────────────────────────────────────────────────────────────

    @BeforeEach
    void injectBucket() {
        ReflectionTestUtils.setField(service, "bucket", "test-bucket");
    }

    private void stubPresigner(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        when(presignedGetObjectRequest.url()).thenReturn(url);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedGetObjectRequest);
    }

    private MockMultipartFile imageFile() {
        return new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg",
                new byte[1024]  // 1 KB
        );
    }

    private MockMultipartFile pdfFile() {
        return new MockMultipartFile(
                "file", "doc.pdf", "application/pdf",
                new byte[512]
        );
    }

    private MockMultipartFile tooBigFile() {
        // 26 MB — over the 25MB limit
        return new MockMultipartFile(
                "file", "big.jpg", "image/jpeg",
                new byte[26 * 1024 * 1024]
        );
    }

    private MockMultipartFile disallowedTypeFile() {
        return new MockMultipartFile(
                "file", "virus.exe", "application/x-msdownload",
                new byte[100]
        );
    }

    private MediaFile savedMedia(String mediaId) {
        return MediaFile.builder()
                .mediaId(mediaId)
                .filename("files/uuid_photo.jpg")
                .originalName("photo.jpg")
                .url("https://s3.example.com/files/uuid_photo.jpg")
                .mimeType("image/jpeg")
                .sizeKb(1L)
                .roomId(10L)
                .messageId("msg-001")
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  validate()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("validate() — file guard")
    class ValidateTests {

        @Test
        @DisplayName("throws when file exceeds 25MB")
        void validate_tooLarge_throws() {
            assertThatThrownBy(() -> service.uploadFile(tooBigFile(), 1L, "m1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("25MB");
        }

        @Test
        @DisplayName("throws when MIME type is not in allowed list")
        void validate_disallowedType_throws() {
            assertThatThrownBy(() -> service.uploadFile(disallowedTypeFile(), 1L, "m1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not allowed");
        }

        @Test
        @DisplayName("throws for text/plain MIME type")
        void validate_textPlain_throws() {
            MockMultipartFile f = new MockMultipartFile("file", "notes.txt", "text/plain", new byte[100]);
            assertThatThrownBy(() -> service.uploadFile(f, 1L, "m1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("accepts image/jpeg without throwing")
        void validate_jpeg_ok() throws Exception {
            stubPresigner("https://s3.example.com/files/photo.jpg");
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());
            when(mediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> service.uploadFile(imageFile(), 1L, "m1"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts application/pdf without throwing")
        void validate_pdf_ok() throws Exception {
            stubPresigner("https://s3.example.com/files/doc.pdf");
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());
            when(mediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> service.uploadFile(pdfFile(), 1L, "m1"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts application/zip without throwing")
        void validate_zip_ok() throws Exception {
            stubPresigner("https://s3.example.com/files/a.zip");
            MockMultipartFile zip = new MockMultipartFile("file", "a.zip", "application/zip", new byte[100]);
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());
            when(mediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> service.uploadFile(zip, null, null))
                    .doesNotThrowAnyException();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  uploadFile()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("uploadFile()")
    class UploadFileTests {

        @Test
        @DisplayName("calls s3Client.putObject once")
        void uploadFile_callsS3PutOnce() throws Exception {
            stubPresigner("https://s3.example.com/test");
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());
            when(mediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.uploadFile(pdfFile(), 1L, "m1");

            // Once for main file (no thumbnail for pdf)
            verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("saves MediaFile to repository and returns it")
        void uploadFile_savesAndReturns() throws Exception {
            stubPresigner("https://s3.example.com/test");
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());
            MediaFile saved = savedMedia("id-1");
            when(mediaRepository.save(any())).thenReturn(saved);

            MediaFile result = service.uploadFile(pdfFile(), 10L, "msg-001");

            assertThat(result).isEqualTo(saved);
        }

        @Test
        @DisplayName("builds MediaFile with correct fields from file and params")
        void uploadFile_correctFields() throws Exception {
            stubPresigner("https://s3.example.com/test");
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            ArgumentCaptor<MediaFile> captor = ArgumentCaptor.forClass(MediaFile.class);
            when(mediaRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.uploadFile(pdfFile(), 5L, "msg-xyz");

            MediaFile saved = captor.getValue();
            assertThat(saved.getOriginalName()).isEqualTo("doc.pdf");
            assertThat(saved.getMimeType()).isEqualTo("application/pdf");
            assertThat(saved.getRoomId()).isEqualTo(5L);
            assertThat(saved.getMessageId()).isEqualTo("msg-xyz");
            assertThat(saved.getSizeKb()).isZero(); // 512 bytes → 0 KB integer division
            assertThat(saved.getUrl()).isEqualTo("https://s3.example.com/test");
        }

        @Test
        @DisplayName("S3 key starts with 'files/' prefix")
        void uploadFile_s3KeyPrefix() throws Exception {
            stubPresigner("https://s3.example.com/test");
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());
            when(mediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
            when(s3Client.putObject(captor.capture(), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            service.uploadFile(pdfFile(), 1L, "m1");

            assertThat(captor.getValue().key()).startsWith("files/");
        }

        @Test
        @DisplayName("wraps S3 exception in RuntimeException")
        void uploadFile_s3Failure_wraps() {
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenThrow(new RuntimeException("S3 down"));

            assertThatThrownBy(() -> service.uploadFile(pdfFile(), 1L, "m1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("S3 upload failed");
        }

        @Test
        @DisplayName("thumbnailUrl is null for PDF (non-image)")
        void uploadFile_pdf_noThumbnail() throws Exception {
            stubPresigner("https://s3.example.com/test");
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            ArgumentCaptor<MediaFile> captor = ArgumentCaptor.forClass(MediaFile.class);
            when(mediaRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.uploadFile(pdfFile(), 1L, "m1");

            assertThat(captor.getValue().getThumbnailUrl()).isNull();
        }

        @Test
        @DisplayName("roomId and messageId can be null")
        void uploadFile_nullParams_ok() throws Exception {
            stubPresigner("https://s3.example.com/test");
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());
            when(mediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> service.uploadFile(pdfFile(), null, null))
                    .doesNotThrowAnyException();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  uploadImage()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("uploadImage()")
    class UploadImageTests {

        @Test
        @DisplayName("throws IllegalArgumentException for non-image MIME type")
        void uploadImage_nonImage_throws() {
            assertThatThrownBy(() -> service.uploadImage(pdfFile(), 1L, "m1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Not an image type");
        }

        @Test
        @DisplayName("throws for application/zip")
        void uploadImage_zip_throws() {
            MockMultipartFile zip = new MockMultipartFile("file", "a.zip", "application/zip", new byte[100]);
            assertThatThrownBy(() -> service.uploadImage(zip, 1L, "m1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("accepts image/jpeg and delegates to uploadFile")
        void uploadImage_jpeg_ok() throws Exception {
            stubPresigner("https://s3.example.com/test");
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());
            when(mediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Should not throw
            assertThatCode(() -> service.uploadImage(imageFile(), 1L, "m1"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts image/png")
        void uploadImage_png_ok() throws Exception {
            stubPresigner("https://s3.example.com/test");
            MockMultipartFile png = new MockMultipartFile("file", "img.png", "image/png", new byte[512]);
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());
            when(mediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> service.uploadImage(png, 1L, "m1"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts image/webp")
        void uploadImage_webp_ok() throws Exception {
            stubPresigner("https://s3.example.com/test");
            MockMultipartFile webp = new MockMultipartFile("file", "img.webp", "image/webp", new byte[512]);
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());
            when(mediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> service.uploadImage(webp, 1L, "m1"))
                    .doesNotThrowAnyException();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  getFileById()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getFileById()")
    class GetFileByIdTests {

        @Test
        @DisplayName("returns refreshed MediaFile when found")
        void getFileById_found() throws Exception {
            stubPresigner("https://s3.example.com/fresh");
            MediaFile media = savedMedia("abc");
            media.setFilename("files/uuid_photo.jpg");
            when(mediaRepository.findByMediaId("abc")).thenReturn(Optional.of(media));

            Optional<MediaFile> result = service.getFileById("abc");

            assertThat(result).isPresent();
            assertThat(result.get().getUrl()).isEqualTo("https://s3.example.com/fresh");
        }

        @Test
        @DisplayName("returns empty Optional when not found")
        void getFileById_notFound() {
            when(mediaRepository.findByMediaId("unknown")).thenReturn(Optional.empty());

            assertThat(service.getFileById("unknown")).isEmpty();
        }

        @Test
        @DisplayName("URL is refreshed (calls presigner)")
        void getFileById_refreshesUrl() throws Exception {
            stubPresigner("https://s3.example.com/refreshed-url");
            MediaFile media = savedMedia("id-1");
            media.setUrl("https://old-expired-url.com");
            when(mediaRepository.findByMediaId("id-1")).thenReturn(Optional.of(media));

            Optional<MediaFile> result = service.getFileById("id-1");

            assertThat(result.get().getUrl()).isEqualTo("https://s3.example.com/refreshed-url");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  getFilesByRoom()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getFilesByRoom()")
    class GetFilesByRoomTests {

        @Test
        @DisplayName("returns list of files with refreshed URLs")
        void getFilesByRoom_returnsList() throws Exception {
            stubPresigner("https://s3.example.com/url");
            MediaFile m1 = savedMedia("id-1");
            MediaFile m2 = savedMedia("id-2");
            when(mediaRepository.findByRoomId(10L)).thenReturn(List.of(m1, m2));

            List<MediaFile> result = service.getFilesByRoom(10L);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when room has no files")
        void getFilesByRoom_empty() {
            when(mediaRepository.findByRoomId(99L)).thenReturn(List.of());

            assertThat(service.getFilesByRoom(99L)).isEmpty();
        }

        @Test
        @DisplayName("each file URL is refreshed via presigner")
        void getFilesByRoom_urlsRefreshed() throws Exception {
            stubPresigner("https://s3.example.com/fresh");
            MediaFile m1 = savedMedia("id-1");
            m1.setUrl("https://expired.url");
            when(mediaRepository.findByRoomId(10L)).thenReturn(List.of(m1));

            List<MediaFile> result = service.getFilesByRoom(10L);

            assertThat(result.get(0).getUrl()).isEqualTo("https://s3.example.com/fresh");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  getFilesByUploader()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getFilesByUploader()")
    class GetFilesByUploaderTests {

        @Test
        @DisplayName("returns list for known uploader")
        void getFilesByUploader_found() throws Exception {
            stubPresigner("https://s3.example.com/url");
            when(mediaRepository.findByUploaderId(5L)).thenReturn(List.of(savedMedia("id-1")));

            assertThat(service.getFilesByUploader(5L)).hasSize(1);
        }

        @Test
        @DisplayName("returns empty list for unknown uploader")
        void getFilesByUploader_empty() {
            when(mediaRepository.findByUploaderId(999L)).thenReturn(List.of());

            assertThat(service.getFilesByUploader(999L)).isEmpty();
        }

        @Test
        @DisplayName("URLs are refreshed for each file")
        void getFilesByUploader_urlsRefreshed() throws Exception {
            stubPresigner("https://s3.example.com/refreshed");
            MediaFile m = savedMedia("id-1");
            m.setUrl("https://stale.url");
            when(mediaRepository.findByUploaderId(5L)).thenReturn(List.of(m));

            List<MediaFile> result = service.getFilesByUploader(5L);

            assertThat(result.get(0).getUrl()).isEqualTo("https://s3.example.com/refreshed");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  deleteFile()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteFile()")
    class DeleteFileTests {

        @Test
        @DisplayName("calls s3 deleteObject for file key")
        void deleteFile_deletesFromS3() {
            MediaFile media = savedMedia("id-del");
            media.setFilename("files/uuid_photo.jpg");
            media.setThumbnailUrl(null);
            when(mediaRepository.findByMediaId("id-del")).thenReturn(Optional.of(media));

            service.deleteFile("id-del");

            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("calls deleteByMediaId on repository")
        void deleteFile_deletesFromRepo() {
            MediaFile media = savedMedia("id-del");
            media.setThumbnailUrl(null);
            when(mediaRepository.findByMediaId("id-del")).thenReturn(Optional.of(media));

            service.deleteFile("id-del");

            verify(mediaRepository).deleteByMediaId("id-del");
        }

        @Test
        @DisplayName("does nothing when mediaId not found")
        void deleteFile_notFound_noOp() {
            when(mediaRepository.findByMediaId("ghost")).thenReturn(Optional.empty());

            service.deleteFile("ghost");

            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
            verify(mediaRepository, never()).deleteByMediaId(any());
        }

        @Test
        @DisplayName("also deletes thumbnail from S3 when thumbnailUrl is present")
        void deleteFile_withThumbnail_deletesThumb() {
            MediaFile media = savedMedia("id-del");
            media.setFilename("files/uuid_photo.jpg");
            media.setThumbnailUrl("https://s3.example.com/thumbs/thumb_uuid_photo.jpg?sig=abc");
            when(mediaRepository.findByMediaId("id-del")).thenReturn(Optional.of(media));

            service.deleteFile("id-del");

            // called twice: once for main file, once for thumbnail
            verify(s3Client, times(2)).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("does not delete thumbnail when thumbnailUrl is null")
        void deleteFile_noThumbnail_onlyOneS3Delete() {
            MediaFile media = savedMedia("id-del");
            media.setThumbnailUrl(null);
            when(mediaRepository.findByMediaId("id-del")).thenReturn(Optional.of(media));

            service.deleteFile("id-del");

            verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("s3 delete failure is swallowed silently")
        void deleteFile_s3Failure_swallowed() {
            MediaFile media = savedMedia("id-del");
            media.setThumbnailUrl(null);
            when(mediaRepository.findByMediaId("id-del")).thenReturn(Optional.of(media));
            when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                    .thenThrow(new RuntimeException("S3 error"));

            assertThatCode(() -> service.deleteFile("id-del")).doesNotThrowAnyException();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  generateThumbnail()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateThumbnail()")
    class GenerateThumbnailTests {

        @Test
        @DisplayName("returns presigned URL for given s3Key")
        void generateThumbnail_returnsPresignedUrl() throws Exception {
            stubPresigner("https://s3.example.com/thumbs/key");

            String result = service.generateThumbnail("thumbs/key");

            assertThat(result).isEqualTo("https://s3.example.com/thumbs/key");
        }

        @Test
        @DisplayName("calls presigner with provided key")
        void generateThumbnail_callsPresigner() throws Exception {
            stubPresigner("https://s3.example.com/thumbs/key");

            service.generateThumbnail("thumbs/key");

            verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  getAllFiles()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllFiles()")
    class GetAllFilesTests {

        @Test
        @DisplayName("returns all files with refreshed URLs")
        void getAllFiles_returnsAll() throws Exception {
            stubPresigner("https://s3.example.com/url");
            when(mediaRepository.findAll())
                    .thenReturn(List.of(savedMedia("id-1"), savedMedia("id-2"), savedMedia("id-3")));

            assertThat(service.getAllFiles()).hasSize(3);
        }

        @Test
        @DisplayName("returns empty list when no files exist")
        void getAllFiles_empty() {
            when(mediaRepository.findAll()).thenReturn(List.of());

            assertThat(service.getAllFiles()).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  getFileCount()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getFileCount()")
    class GetFileCountTests {

        @Test
        @DisplayName("returns count from repository")
        void getFileCount_returnsCount() {
            when(mediaRepository.countByRoomId(10L)).thenReturn(7);

            assertThat(service.getFileCount(10L)).isEqualTo(7);
        }

        @Test
        @DisplayName("returns 0 when no files in room")
        void getFileCount_zero() {
            when(mediaRepository.countByRoomId(99L)).thenReturn(0);

            assertThat(service.getFileCount(99L)).isZero();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  extractKey() — indirect test via deleteFile thumbnail branch
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("extractKey() — via delete thumbnail")
    class ExtractKeyTests {

        @Test
        @DisplayName("correctly extracts filename from URL before query string")
        void extractKey_stripsQueryString() {
            MediaFile media = savedMedia("id-1");
            media.setFilename("files/uuid_photo.jpg");
            media.setThumbnailUrl("https://bucket.s3.amazonaws.com/thumbs/thumb_uuid_photo.jpg?X-Amz-Signature=abc123");
            when(mediaRepository.findByMediaId("id-1")).thenReturn(Optional.of(media));

            ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
            service.deleteFile("id-1");

            verify(s3Client, times(2)).deleteObject(captor.capture());
            // Second call = thumbnail; key should be "thumbs/thumb_<extracted>"
            String thumbKey = captor.getAllValues().get(1).key();
            assertThat(thumbKey).doesNotContain("?");
            assertThat(thumbKey).startsWith("thumbs/thumb_");
        }
    }
}
