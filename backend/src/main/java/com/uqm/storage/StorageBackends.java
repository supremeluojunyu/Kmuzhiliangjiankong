package com.uqm.storage;

import com.uqm.common.BusinessException;
import com.uqm.dto.StorageSettingsDto;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class LocalStorageBackend implements StorageProvider {

    private final String basePath;

    LocalStorageBackend(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public void store(MultipartFile file, String storedKey) {
        try {
            Path dir = Paths.get(basePath);
            Files.createDirectories(dir);
            Path target = dir.resolve(storedKey).normalize();
            if (!target.startsWith(dir.toAbsolutePath().normalize())) {
                throw new BusinessException(400, "非法路径");
            }
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败");
        }
    }

    @Override
    public Resource load(String storedKey) {
        Path file = resolve(storedKey);
        try {
            return new UrlResource(file.toUri());
        } catch (Exception e) {
            throw new BusinessException(404, "文件不存在");
        }
    }

    @Override
    public boolean exists(String storedKey) {
        return Files.exists(resolve(storedKey));
    }

    void testConnection() {
        try {
            Path dir = Paths.get(basePath).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path probe = dir.resolve(".storage_probe");
            if (!probe.normalize().startsWith(dir)) {
                throw new BusinessException(500, "本地存储路径无效");
            }
            Files.writeString(probe, "ok");
            Files.deleteIfExists(probe);
        } catch (IOException e) {
            throw new BusinessException(500, "本地存储不可写: " + e.getMessage());
        }
    }

    private Path resolve(String storedKey) {
        Path dir = Paths.get(basePath).toAbsolutePath().normalize();
        Path file = dir.resolve(storedKey).normalize();
        if (!file.startsWith(dir)) {
            throw new BusinessException(400, "非法文件名");
        }
        if (!Files.exists(file)) {
            throw new BusinessException(404, "文件不存在");
        }
        return file;
    }
}

final class S3StorageBackend implements StorageProvider {

    private final StorageSettingsDto settings;
    private final S3Client s3Client;

    S3StorageBackend(StorageSettingsDto settings) {
        this.settings = settings;
        if (!StringUtils.hasText(settings.getS3Bucket())) {
            throw new BusinessException(400, "S3 Bucket 未配置");
        }
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(settings.isS3PathStyleAccess())
                .build();
        var builder = S3Client.builder()
                .region(Region.of(settings.getS3Region()))
                .serviceConfiguration(s3Config);
        if (StringUtils.hasText(settings.getS3Endpoint())) {
            builder.endpointOverride(URI.create(settings.getS3Endpoint()));
        }
        if (StringUtils.hasText(settings.getS3AccessKey()) && StringUtils.hasText(settings.getS3SecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(settings.getS3AccessKey(), settings.getS3SecretKey())));
        }
        this.s3Client = builder.build();
        ensureBucket(settings.getS3Bucket());
    }

    @Override
    public void store(MultipartFile file, String storedKey) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(settings.getS3Bucket())
                    .key(storedKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new BusinessException(500, "文件上传失败");
        }
    }

    @Override
    public Resource load(String storedKey) {
        try {
            var response = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(settings.getS3Bucket())
                    .key(storedKey)
                    .build());
            return new InputStreamResource(response);
        } catch (NoSuchKeyException e) {
            throw new BusinessException(404, "文件不存在");
        }
    }

    @Override
    public boolean exists(String storedKey) {
        try {
            s3Client.headObject(b -> b.bucket(settings.getS3Bucket()).key(storedKey));
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    void testConnection() {
        s3Client.headBucket(HeadBucketRequest.builder().bucket(settings.getS3Bucket()).build());
    }

    void close() {
        s3Client.close();
    }

    private void ensureBucket(String bucket) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(b -> b.bucket(bucket));
        }
    }
}
