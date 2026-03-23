package com.nchuy099.mini_instagram.media.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3MediaServiceImplTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private S3Presigner s3Presigner;

    private S3MediaServiceImpl mediaService;

    private static final String BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setUp() {
        mediaService = new S3MediaServiceImpl(s3Client, s3Presigner);
        ReflectionTestUtils.setField(mediaService, "bucketName", BUCKET_NAME);
    }

    @Test
    void generatePresignedUrl_ShouldReturnUrl() throws MalformedURLException {
        String fileName = "test.jpg";
        String contentType = "image/jpeg";
        URL mockUrl = new URL("http://example.com/presigned");
        
        PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
        when(mockPresignedRequest.url()).thenReturn(mockUrl);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedRequest);

        String result = mediaService.generatePresignedUrl(fileName, contentType);

        assertThat(result).isEqualTo("http://example.com/presigned");
        
        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());
        
        PutObjectPresignRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.putObjectRequest().bucket()).isEqualTo(BUCKET_NAME);
        assertThat(capturedRequest.putObjectRequest().contentType()).isEqualTo(contentType);
        assertThat(capturedRequest.putObjectRequest().key()).contains(fileName);
    }

    @Test
    void deleteFile_ShouldCallS3Client() {
        String fileUrl = "https://s3.amazonaws.com/test-bucket/some-uuid_test.jpg";

        mediaService.deleteFile(fileUrl);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());

        DeleteObjectRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo(BUCKET_NAME);
        assertThat(capturedRequest.key()).isEqualTo("some-uuid_test.jpg");
    }
}
