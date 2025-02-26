package projecteval.springuploads3.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import projecteval.springuploads3.service.StorageService;
import projecteval.springuploads3.service.model.DownloadedResource;

@ExtendWith(MockitoExtension.class)
class FileDownloadControllerTest {

    @Mock
    private StorageService storageService;

    @InjectMocks
    private FileDownloadController fileDownloadController;

    @Test
    @DisplayName("Should download file and return correct ResponseEntity")
    void testDownload() {
        String id = "testKey";

        DownloadedResource resource = DownloadedResource.builder()
                .id(id)
                .fileName("testKey.txt")
                .contentLength(123L)
                .inputStream(new ByteArrayInputStream("testContent".getBytes()))
                .build();

        when(storageService.download(id)).thenReturn(resource);

        ResponseEntity<Resource> response = fileDownloadController.download(id);

        assertNotNull(response);
        assertTrue(response.getHeaders().containsKey(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("attachment; filename=testKey.txt", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(123L, response.getHeaders().getContentLength());
    }
}
