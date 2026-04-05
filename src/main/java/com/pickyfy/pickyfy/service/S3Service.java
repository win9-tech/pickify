package com.pickyfy.pickyfy.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3Service {

    private final AmazonS3Client amazonS3Client;

    private static final long PRESIGNED_URL_EXPIRATION_MS = 1000 * 60 * 60; // 1시간

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.path.image}")
    private String imageFolder;

    @Value("${cloud.aws.s3.endpoint:}")
    private String endpoint;

    @Value("${cloud.aws.s3.public-url:}")
    private String publicUrl;

    /**
     * 파일 업로드 후 S3 키(경로) 반환
     */
    public String upload(MultipartFile multipartFile) {
        String key = imageFolder + generateUniqueFileName(Objects.requireNonNull(multipartFile.getOriginalFilename()));

        if (!(key.endsWith(".png") || key.endsWith(".jpg") || key.endsWith(".jpeg") || key.endsWith(
                ".gif") || key.endsWith(".bmp"))) {
            throw new RuntimeException();
        }

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(multipartFile.getContentType());
        metadata.setContentLength(multipartFile.getSize());
        return putS3(multipartFile, key, metadata);
    }

    private String putS3(MultipartFile multipartFile, String key, ObjectMetadata metadata) {
        try (InputStream inputStream = multipartFile.getInputStream()) {
            amazonS3Client.putObject(
                    new PutObjectRequest(bucket, key, inputStream, metadata)
            );
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류 발생", e);
        }
        return key;  // 키만 반환
    }

    /**
     * S3 키로 presigned URL 생성 (1시간 유효)
     * 이미 URL 형태(http/https)인 경우 그대로 반환 (OAuth 프로필 이미지 등)
     */
    public String generatePresignedUrl(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        // 이미 URL 형태면 그대로 반환 (카카오 등 외부 프로필 이미지)
        if (key.startsWith("http://") || key.startsWith("https://")) {
            return key;
        }

        Date expiration = new Date(System.currentTimeMillis() + PRESIGNED_URL_EXPIRATION_MS);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);

        URL url = amazonS3Client.generatePresignedUrl(request);
        String presignedUrl = url.toString();

        // 내부 엔드포인트를 외부 URL로 치환
        if (publicUrl != null && !publicUrl.isEmpty() && endpoint != null && !endpoint.isEmpty()) {
            presignedUrl = presignedUrl.replace(endpoint, publicUrl);
        }

        return presignedUrl;
    }

    private String generateUniqueFileName(String originalFilename) {
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return UUID.randomUUID() + extension;
    }

    /**
     * S3 키로 파일 삭제
     */
    public void removeFile(String key) {
        if (key != null && !key.isEmpty()) {
            amazonS3Client.deleteObject(bucket, key);
        }
    }
}