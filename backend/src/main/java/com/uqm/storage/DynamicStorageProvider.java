package com.uqm.storage;

import com.uqm.common.BusinessException;
import com.uqm.config.UploadProperties;
import com.uqm.dto.StorageSettingsDto;
import com.uqm.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

@Component
@Primary
@RequiredArgsConstructor
public class DynamicStorageProvider implements StorageProvider {

    private final SystemConfigService systemConfigService;
    private final UploadProperties uploadProperties;

    private volatile StorageProvider delegate;
    private volatile String delegateKey;
    private volatile S3StorageBackend s3Backend;

    @Override
    public void store(MultipartFile file, String storedKey) {
        resolve().store(file, storedKey);
    }

    @Override
    public Resource load(String storedKey) {
        return resolve().load(storedKey);
    }

    @Override
    public boolean exists(String storedKey) {
        return resolve().exists(storedKey);
    }

    public void testConnection() {
        StorageSettingsDto settings = effectiveSettings();
        if ("s3".equals(settings.getType())) {
            S3StorageBackend backend = new S3StorageBackend(settings);
            try {
                backend.testConnection();
            } finally {
                backend.close();
            }
        } else {
            new LocalStorageBackend(settings.getLocalPath()).testConnection();
        }
        invalidate();
    }

    public void invalidate() {
        if (s3Backend != null) {
            s3Backend.close();
            s3Backend = null;
        }
        delegate = null;
        delegateKey = null;
    }

    private StorageProvider resolve() {
        StorageSettingsDto settings = effectiveSettings();
        String key = fingerprint(settings);
        if (delegate != null && key.equals(delegateKey)) {
            return delegate;
        }
        synchronized (this) {
            if (delegate != null && key.equals(delegateKey)) {
                return delegate;
            }
            if (s3Backend != null) {
                s3Backend.close();
                s3Backend = null;
            }
            if ("s3".equals(settings.getType())) {
                s3Backend = new S3StorageBackend(settings);
                delegate = s3Backend;
            } else {
                delegate = new LocalStorageBackend(settings.getLocalPath());
            }
            delegateKey = key;
            return delegate;
        }
    }

    StorageSettingsDto effectiveSettings() {
        StorageSettingsDto db = systemConfigService.getStorage();
        StorageSettingsDto merged = new StorageSettingsDto();
        merged.setType(StringUtils.hasText(db.getType()) ? db.getType() : uploadProperties.getType());
        merged.setLocalPath(StringUtils.hasText(db.getLocalPath()) ? db.getLocalPath() : uploadProperties.getPath());
        merged.setS3Endpoint(pick(db.getS3Endpoint(), uploadProperties.getS3().getEndpoint()));
        merged.setS3Region(pick(db.getS3Region(), uploadProperties.getS3().getRegion()));
        merged.setS3Bucket(pick(db.getS3Bucket(), uploadProperties.getS3().getBucket()));
        merged.setS3AccessKey(pick(db.getS3AccessKey(), uploadProperties.getS3().getAccessKey()));
        merged.setS3SecretKey(pick(db.getS3SecretKey(), uploadProperties.getS3().getSecretKey()));
        merged.setS3PathStyleAccess(db.isS3PathStyleAccess());
        if (!StringUtils.hasText(merged.getType())) {
            merged.setType("local");
        }
        return merged;
    }

    private String pick(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private String fingerprint(StorageSettingsDto s) {
        return s.getType() + "|" + s.getLocalPath() + "|" + s.getS3Endpoint() + "|" + s.getS3Region()
                + "|" + s.getS3Bucket() + "|" + s.getS3AccessKey() + "|" + s.getS3SecretKey()
                + "|" + s.isS3PathStyleAccess();
    }
}
