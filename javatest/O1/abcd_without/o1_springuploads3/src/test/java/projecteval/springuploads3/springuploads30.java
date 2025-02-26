package projecteval.springuploads3.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import projecteval.springuploads3.service.model.DownloadedResource;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private AmazonS3 amazonS3;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private S3StorageService s3StorageService;

    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        // Force a new instance to test bucket initialization logic
        s3StorageService = new S3StorageService(amazonS3, bucketName);
    }

    @Test
    @DisplayName("Should create bucket if it does not exist")
    void testBucketInitialization_CreatesBucketIfNotExist() {
        when(amazonS3.doesBucketExistV2(bucketName)).thenReturn(false);

        new S3StorageService(amazonS3, bucketName);

        verify(amazonS3, times(1)).createBucket(bucketName);
    }

    @Test
    @DisplayName("Should not create bucket if it already exists")
    void testBucketInitialization_DoesNotCreateBucketIfExist() {
        when(amazonS3.doesBucketExistV2(bucketName)).thenReturn(true);

        new S3StorageService(amazonS3, bucketName);

        verify(amazonS3, never()).createBucket(bucketName);
    }

    @Nested
    @DisplayName("Upload Tests")
    class UploadTests {

        @Test
        @DisplayName("Should upload file and return generated key")
        void testUploadSuccess() throws Exception {
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
            when(multipartFile.getSize()).thenReturn(4L);
            when(multipartFile.getContentType()).thenReturn("text/plain");
            when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
            when(amazonS3.doesBucketExistV2(bucketName)).thenReturn(true);

            String key = s3StorageService.upload(multipartFile);

            assertNotNull(key);
            assertTrue(key.length() == 50, "Returned key should have length 50 (randomAlphanumeric(50))");

            // Verify putObject is called
            verify(amazonS3, times(1)).putObject(eq(bucketName), eq(key), any(InputStream.class), any(ObjectMetadata.class));
        }
    }

    @Nested
    @DisplayName("Download Tests")
    class DownloadTests {

        @Test
        @DisplayName("Should download file successfully")
        void testDownloadSuccess() {
            String id = RandomStringUtils.randomAlphanumeric(10);

            // Mock S3 Object
            S3Object s3Object = new S3Object();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(10L);
            metadata.addUserMetadata("fileExtension", "txt");
            s3Object.setObjectMetadata(metadata);
            s3Object.setObjectContent(new ByteArrayInputStream("some content".getBytes()));

            when(amazonS3.getObject(bucketName, id)).thenReturn(s3Object);

            DownloadedResource resource = s3StorageService.download(id);

            assertNotNull(resource);
            assertEquals(id, resource.getId());
            assertEquals(id + ".txt", resource.getFileName());
            assertEquals(10L, resource.getContentLength());
        }
    }
}
