package com.talkifyx.media_service.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.talkifyx.media_service.entity.MediaFile;
import com.talkifyx.media_service.service.MediaService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MediaResourceTest {

    @Mock MediaService mediaService;
    @InjectMocks MediaResource mediaResource;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(mediaResource).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private MediaFile sampleMedia(String id) {
        return MediaFile.builder()
                .mediaId(id)
                .uploaderId(1L)
                .roomId(10L)
                .messageId("msg-001")
                .filename("files/uuid_photo.jpg")
                .originalName("photo.jpg")
                .url("https://s3.example.com/photo.jpg")
                .thumbnailUrl("https://s3.example.com/thumb.jpg")
                .mimeType("image/jpeg")
                .sizeKb(512L)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    private MockMultipartFile mockFile(String mime) {
        return new MockMultipartFile("file", "photo.jpg", mime, new byte[512]);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  POST /api/media/upload
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/media/upload")
    class UploadFileEndpoint {

        @Test
        @DisplayName("200 OK with saved media body")
        void upload_returns200() throws Exception {
            MediaFile saved = sampleMedia("id-1");
            when(mediaService.uploadFile(any(), any(), any())).thenReturn(saved);

            mockMvc.perform(multipart("/api/media/upload")
                            .file(mockFile("application/pdf"))
                            .header("X-User-Id", "42")
                            .param("roomId", "10")
                            .param("messageId", "msg-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mediaId").value("id-1"));
        }

        @Test
        @DisplayName("sets uploaderId from X-User-Id header on returned object")
        void upload_setsUploaderId() throws Exception {
            MediaFile saved = sampleMedia("id-1");
            saved.setUploaderId(null); // start null, resource sets it
            when(mediaService.uploadFile(any(), any(), any())).thenReturn(saved);

            mockMvc.perform(multipart("/api/media/upload")
                            .file(mockFile("application/pdf"))
                            .header("X-User-Id", "99")
                            .param("roomId", "10"))
                    .andExpect(jsonPath("$.uploaderId").value(99));
        }

        @Test
        @DisplayName("works without optional roomId and messageId params")
        void upload_noOptionalParams() throws Exception {
            when(mediaService.uploadFile(any(), isNull(), isNull())).thenReturn(sampleMedia("id-1"));

            mockMvc.perform(multipart("/api/media/upload")
                            .file(mockFile("application/pdf"))
                            .header("X-User-Id", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("passes roomId and messageId to service")
        void upload_passesParams() throws Exception {
            when(mediaService.uploadFile(any(), eq(10L), eq("msg-001"))).thenReturn(sampleMedia("id-1"));

            mockMvc.perform(multipart("/api/media/upload")
                            .file(mockFile("application/pdf"))
                            .header("X-User-Id", "1")
                            .param("roomId", "10")
                            .param("messageId", "msg-001"))
                    .andExpect(status().isOk());

            verify(mediaService).uploadFile(any(), eq(10L), eq("msg-001"));
        }

        @Test
        @DisplayName("returns response fields: url, mimeType, sizeKb, originalName")
        void upload_responseFields() throws Exception {
            when(mediaService.uploadFile(any(), any(), any())).thenReturn(sampleMedia("id-1"));

            mockMvc.perform(multipart("/api/media/upload")
                            .file(mockFile("application/pdf"))
                            .header("X-User-Id", "1"))
                    .andExpect(jsonPath("$.url").value("https://s3.example.com/photo.jpg"))
                    .andExpect(jsonPath("$.mimeType").value("image/jpeg"))
                    .andExpect(jsonPath("$.sizeKb").value(512))
                    .andExpect(jsonPath("$.originalName").value("photo.jpg"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  POST /api/media/upload/image
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/media/upload/image")
    class UploadImageEndpoint {

        @Test
        @DisplayName("200 OK for image upload")
        void uploadImage_returns200() throws Exception {
            when(mediaService.uploadImage(any(), any(), any())).thenReturn(sampleMedia("id-img"));

            mockMvc.perform(multipart("/api/media/upload/image")
                            .file(mockFile("image/jpeg"))
                            .header("X-User-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mediaId").value("id-img"));
        }

        @Test
        @DisplayName("sets uploaderId from header")
        void uploadImage_setsUploaderId() throws Exception {
            MediaFile saved = sampleMedia("id-img");
            saved.setUploaderId(null);
            when(mediaService.uploadImage(any(), any(), any())).thenReturn(saved);

            mockMvc.perform(multipart("/api/media/upload/image")
                            .file(mockFile("image/png"))
                            .header("X-User-Id", "77"))
                    .andExpect(jsonPath("$.uploaderId").value(77));
        }

        @Test
        @DisplayName("response includes thumbnailUrl")
        void uploadImage_hasThumbnailUrl() throws Exception {
            when(mediaService.uploadImage(any(), any(), any())).thenReturn(sampleMedia("id-img"));

            mockMvc.perform(multipart("/api/media/upload/image")
                            .file(mockFile("image/jpeg"))
                            .header("X-User-Id", "1"))
                    .andExpect(jsonPath("$.thumbnailUrl").value("https://s3.example.com/thumb.jpg"));
        }

        @Test
        @DisplayName("calls mediaService.uploadImage (not uploadFile)")
        void uploadImage_callsCorrectServiceMethod() throws Exception {
            when(mediaService.uploadImage(any(), any(), any())).thenReturn(sampleMedia("id-img"));

            mockMvc.perform(multipart("/api/media/upload/image")
                    .file(mockFile("image/jpeg"))
                    .header("X-User-Id", "1"));

            verify(mediaService).uploadImage(any(), any(), any());
            verify(mediaService, never()).uploadFile(any(), any(), any());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET /api/media/{mediaId}
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/media/{mediaId}")
    class GetByIdEndpoint {

        @Test
        @DisplayName("200 OK when file found")
        void getById_found() throws Exception {
            when(mediaService.getFileById("id-1")).thenReturn(Optional.of(sampleMedia("id-1")));

            mockMvc.perform(get("/api/media/id-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mediaId").value("id-1"));
        }

        @Test
        @DisplayName("404 Not Found when file absent")
        void getById_notFound() throws Exception {
            when(mediaService.getFileById("missing")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/media/missing"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns fresh URL in response")
        void getById_freshUrl() throws Exception {
            MediaFile media = sampleMedia("id-1");
            media.setUrl("https://fresh.url");
            when(mediaService.getFileById("id-1")).thenReturn(Optional.of(media));

            mockMvc.perform(get("/api/media/id-1"))
                    .andExpect(jsonPath("$.url").value("https://fresh.url"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET /api/media/room/{roomId}
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/media/room/{roomId}")
    class GetByRoomEndpoint {

        @Test
        @DisplayName("200 OK with list of files")
        void getByRoom_returns200() throws Exception {
            when(mediaService.getFilesByRoom(10L))
                    .thenReturn(List.of(sampleMedia("id-1"), sampleMedia("id-2")));

            mockMvc.perform(get("/api/media/room/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("returns empty array when room has no files")
        void getByRoom_empty() throws Exception {
            when(mediaService.getFilesByRoom(99L)).thenReturn(List.of());

            mockMvc.perform(get("/api/media/room/99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("passes correct roomId to service")
        void getByRoom_passesId() throws Exception {
            when(mediaService.getFilesByRoom(42L)).thenReturn(List.of());

            mockMvc.perform(get("/api/media/room/42"));

            verify(mediaService).getFilesByRoom(42L);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET /api/media/uploader/{uploaderId}
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/media/uploader/{uploaderId}")
    class GetByUploaderEndpoint {

        @Test
        @DisplayName("200 OK with list of files")
        void getByUploader_returns200() throws Exception {
            when(mediaService.getFilesByUploader(5L)).thenReturn(List.of(sampleMedia("id-1")));

            mockMvc.perform(get("/api/media/uploader/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("returns empty list for uploader with no files")
        void getByUploader_empty() throws Exception {
            when(mediaService.getFilesByUploader(999L)).thenReturn(List.of());

            mockMvc.perform(get("/api/media/uploader/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DELETE /api/media/{mediaId}
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/media/{mediaId}")
    class DeleteEndpoint {

        @Test
        @DisplayName("204 No Content on successful delete")
        void delete_returns204() throws Exception {
            doNothing().when(mediaService).deleteFile("id-del");

            mockMvc.perform(delete("/api/media/id-del"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("calls service.deleteFile with correct mediaId")
        void delete_callsService() throws Exception {
            mockMvc.perform(delete("/api/media/abc-123"));

            verify(mediaService).deleteFile("abc-123");
        }

        @Test
        @DisplayName("response body is empty")
        void delete_emptyBody() throws Exception {
            mockMvc.perform(delete("/api/media/id-del"))
                    .andExpect(content().string(""));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET /api/media/all
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/media/all")
    class GetAllEndpoint {

        @Test
        @DisplayName("200 OK with all files")
        void getAll_returns200() throws Exception {
            when(mediaService.getAllFiles())
                    .thenReturn(List.of(sampleMedia("id-1"), sampleMedia("id-2"), sampleMedia("id-3")));

            mockMvc.perform(get("/api/media/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3));
        }

        @Test
        @DisplayName("returns empty array when no files")
        void getAll_empty() throws Exception {
            when(mediaService.getAllFiles()).thenReturn(List.of());

            mockMvc.perform(get("/api/media/all"))
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET /api/media/count/{roomId}
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/media/count/{roomId}")
    class GetCountEndpoint {

        @Test
        @DisplayName("200 OK with integer count")
        void getCount_returns200() throws Exception {
            when(mediaService.getFileCount(10L)).thenReturn(5);

            mockMvc.perform(get("/api/media/count/10"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("5"));
        }

        @Test
        @DisplayName("returns 0 when no files in room")
        void getCount_zero() throws Exception {
            when(mediaService.getFileCount(99L)).thenReturn(0);

            mockMvc.perform(get("/api/media/count/99"))
                    .andExpect(content().string("0"));
        }

        @Test
        @DisplayName("passes correct roomId to service")
        void getCount_passesId() throws Exception {
            when(mediaService.getFileCount(77L)).thenReturn(3);

            mockMvc.perform(get("/api/media/count/77"));

            verify(mediaService).getFileCount(77L);
        }
    }
}
