// springuploads3/service/StorageService.java

package projecteval.springuploads3.service;

import org.springframework.web.multipart.MultipartFile;

import projecteval.springuploads3.service.model.DownloadedResource;

public interface StorageService {
    
    String upload(MultipartFile multipartFile);
    
    DownloadedResource download(String id);
}


// springuploads3/service/model/DownloadedResource.java

package projecteval.springuploads3.service.model;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

@Data
@Builder
public class DownloadedResource {
    
    private String id;
    private String fileName;
    private Long contentLength;
    private InputStream inputStream;
}


// springuploads3/service/S3StorageService.java

package projecteval.springuploads3.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import projecteval.springuploads3.service.model.DownloadedResource;
import lombok.SneakyThrows;

@Service
public class S3StorageService implements StorageService {
    
    private static final String FILE_EXTENSION = "fileExtension";
    
    private final AmazonS3 amazonS3;
    private final String bucketName;
    
    public S3StorageService(AmazonS3 amazonS3, @Value("${aws.s3.bucket-name}") String bucketName) {
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
        
        initializeBucket();
    }
    
    @SneakyThrows
    @Override
    public String upload(MultipartFile multipartFile) {
        String key = RandomStringUtils.randomAlphanumeric(50);
        
        amazonS3.putObject(bucketName, key, multipartFile.getInputStream(), extractObjectMetadata(multipartFile));
        
        return key;
    }
    
    @Override
    public DownloadedResource download(String id) {
        S3Object s3Object = amazonS3.getObject(bucketName, id);
        String filename = id + "." + s3Object.getObjectMetadata().getUserMetadata().get(FILE_EXTENSION);
        Long contentLength = s3Object.getObjectMetadata().getContentLength();
        
        return DownloadedResource.builder().id(id).fileName(filename).contentLength(contentLength).inputStream(s3Object.getObjectContent())
                .build();
    }
    
    private ObjectMetadata extractObjectMetadata(MultipartFile file) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        
        objectMetadata.setContentLength(file.getSize());
        objectMetadata.setContentType(file.getContentType());
        
        objectMetadata.getUserMetadata().put(FILE_EXTENSION, FilenameUtils.getExtension(file.getOriginalFilename()));
        
        return objectMetadata;
    }
    
    private void initializeBucket() {
        if (!amazonS3.doesBucketExistV2(bucketName)) {
            amazonS3.createBucket(bucketName);
        }
    }
}


// springuploads3/controller/FileUploadController.java

package projecteval.springuploads3.controller;

import projecteval.springuploads3.service.StorageService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@Log4j2
public class FileUploadController {
    
    private final StorageService storageService;
    
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }
    
    @PostMapping(value = "/upload", produces = "application/json")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        String key = storageService.upload(file);
        return new ResponseEntity<>(key, HttpStatus.OK);
    }
}


// springuploads3/controller/FileDownloadController.java

package projecteval.springuploads3.controller;

import projecteval.springuploads3.service.model.DownloadedResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import projecteval.springuploads3.service.StorageService;
import lombok.extern.log4j.Log4j2;

@Controller
@Log4j2
public class FileDownloadController {
    
    private final StorageService storageService;
    
    public FileDownloadController(StorageService storageService) {
        this.storageService = storageService;
    }
    
    @GetMapping("/download")
    public ResponseEntity<Resource> download(String id) {
        DownloadedResource downloadedResource = storageService.download(id);
        
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + downloadedResource.getFileName())
                .contentLength(downloadedResource.getContentLength()).contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(downloadedResource.getInputStream()));
    }
}


// springuploads3/SpringUploadS3Application.java

package projecteval.springuploads3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringUploadS3Application {

	public static void main(String[] args) {
		SpringApplication.run(SpringUploadS3Application.class, args);
	}

}


// springuploads3/configuration/S3ClientConfiguration.java

package projecteval.springuploads3.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration
public class S3ClientConfiguration {
    
    @Value("${aws.s3.endpoint-url}")
    private String endpointUrl;
    
    @Bean
    AmazonS3 amazonS3() {
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endpointUrl,
                Regions.US_EAST_1.getName());
        return AmazonS3ClientBuilder.standard().withEndpointConfiguration(endpointConfiguration).withPathStyleAccessEnabled(true).build();
    }
}


