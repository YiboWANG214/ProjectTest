package projecteval.springuploads3.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import projecteval.springuploads3.service.StorageService;

@ExtendWith(MockitoExtension.class)
class FileUploadControllerTest {

    @Mock
    private StorageService storageService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileUploadController fileUploadController;

    @Test
    @DisplayName("Should upload file and return generated key in response")
    void testUpload() {
        String expectedKey = "abc123";
        when(storageService.upload(multipartFile)).thenReturn(expectedKey);

        ResponseEntity<String> response = fileUploadController.upload(multipartFile);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedKey, response.getBody());
    }
}
