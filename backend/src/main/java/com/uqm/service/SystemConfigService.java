package com.uqm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uqm.common.BusinessException;
import com.uqm.dto.AuthSettingsDto;
import com.uqm.dto.NotificationSettingsDto;
import com.uqm.dto.PublicAuthConfigVo;
import com.uqm.dto.RetentionSettingsDto;
import com.uqm.dto.StorageSettingsDto;
import com.uqm.dto.SystemSettingsVo;
import com.uqm.entity.SystemConfig;
import com.uqm.mapper.SystemConfigMapper;
import com.uqm.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    public static final String KEY_AUTH = "auth";
    public static final String KEY_NOTIFICATION = "notification";
    public static final String KEY_RETENTION = "retention";
    public static final String KEY_STORAGE = "storage";
    private static final String MASK = "******";

    private final SystemConfigMapper configMapper;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;

    public SystemSettingsVo getSettings(LoginUser user) {
        permissionService.requirePermission(user, "system:config");
        return SystemSettingsVo.builder()
                .auth(maskAuthSecrets(getAuth()))
                .notification(maskNotificationSecrets(getNotification()))
                .retention(getRetention())
                .storage(maskStorageSecrets(getStorage()))
                .build();
    }

    @Transactional
    public SystemSettingsVo updateSettings(LoginUser user, SystemSettingsVo request) {
        permissionService.requirePermission(user, "system:config");
        if (request.getAuth() != null) {
            saveAuth(mergeAuthSecrets(request.getAuth()), user.getUserId());
        }
        if (request.getNotification() != null) {
            saveNotification(mergeNotificationSecrets(request.getNotification()), user.getUserId());
        }
        if (request.getRetention() != null) {
            saveRetention(request.getRetention(), user.getUserId());
        }
        if (request.getStorage() != null) {
            saveStorage(mergeStorageSecrets(request.getStorage()), user.getUserId());
        }
        return getSettings(user);
    }

    public PublicAuthConfigVo getPublicAuthConfig() {
        AuthSettingsDto auth = getAuth();
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return PublicAuthConfigVo.builder()
                .externalAuthEnabled(auth.isEnabled() && !"local".equals(auth.getProvider()))
                .provider(auth.getProvider())
                .localLoginEnabled(!auth.isEnabled() || auth.isLocalLoginEnabled())
                .casLoginUrl(auth.isEnabled() && "cas".equals(auth.getProvider())
                        ? base + "/api/auth/cas/login" : null)
                .oauthLoginUrl(auth.isEnabled() && "oauth2".equals(auth.getProvider())
                        ? base + "/api/auth/oauth2/login" : null)
                .build();
    }

    public AuthSettingsDto getAuth() {
        return read(KEY_AUTH, AuthSettingsDto.class, defaultAuth());
    }

    public NotificationSettingsDto getNotification() {
        return read(KEY_NOTIFICATION, NotificationSettingsDto.class, defaultNotification());
    }

    public RetentionSettingsDto getRetention() {
        return read(KEY_RETENTION, RetentionSettingsDto.class, defaultRetention());
    }

    public StorageSettingsDto getStorage() {
        return read(KEY_STORAGE, StorageSettingsDto.class, defaultStorage());
    }

    public void saveAuth(AuthSettingsDto dto, Integer userId) {
        write(KEY_AUTH, dto, userId);
    }

    public void saveNotification(NotificationSettingsDto dto, Integer userId) {
        write(KEY_NOTIFICATION, dto, userId);
    }

    public void saveRetention(RetentionSettingsDto dto, Integer userId) {
        write(KEY_RETENTION, dto, userId);
    }

    public void saveStorage(StorageSettingsDto dto, Integer userId) {
        write(KEY_STORAGE, dto, userId);
    }

    private StorageSettingsDto mergeStorageSecrets(StorageSettingsDto incoming) {
        StorageSettingsDto current = getStorage();
        if (MASK.equals(incoming.getS3SecretKey()) || !StringUtils.hasText(incoming.getS3SecretKey())) {
            incoming.setS3SecretKey(current.getS3SecretKey());
        }
        return incoming;
    }

    private AuthSettingsDto mergeAuthSecrets(AuthSettingsDto incoming) {
        AuthSettingsDto current = getAuth();
        if (MASK.equals(incoming.getOauthClientSecret()) || !StringUtils.hasText(incoming.getOauthClientSecret())) {
            incoming.setOauthClientSecret(current.getOauthClientSecret());
        }
        return incoming;
    }

    private NotificationSettingsDto mergeNotificationSecrets(NotificationSettingsDto incoming) {
        NotificationSettingsDto current = getNotification();
        if (MASK.equals(incoming.getSmtpPassword()) || !StringUtils.hasText(incoming.getSmtpPassword())) {
            incoming.setSmtpPassword(current.getSmtpPassword());
        }
        if (MASK.equals(incoming.getWechatSecret()) || !StringUtils.hasText(incoming.getWechatSecret())) {
            incoming.setWechatSecret(current.getWechatSecret());
        }
        return incoming;
    }

    private AuthSettingsDto maskAuthSecrets(AuthSettingsDto dto) {
        if (StringUtils.hasText(dto.getOauthClientSecret())) {
            dto.setOauthClientSecret(MASK);
        }
        return dto;
    }

    private NotificationSettingsDto maskNotificationSecrets(NotificationSettingsDto dto) {
        if (StringUtils.hasText(dto.getSmtpPassword())) {
            dto.setSmtpPassword(MASK);
        }
        if (StringUtils.hasText(dto.getWechatSecret())) {
            dto.setWechatSecret(MASK);
        }
        return dto;
    }

    private StorageSettingsDto maskStorageSecrets(StorageSettingsDto dto) {
        if (StringUtils.hasText(dto.getS3SecretKey())) {
            dto.setS3SecretKey(MASK);
        }
        return dto;
    }

    private <T> T read(String key, Class<T> type, T defaultValue) {
        SystemConfig row = configMapper.selectById(key);
        if (row == null || !StringUtils.hasText(row.getConfigValue())) {
            return defaultValue;
        }
        try {
            return objectMapper.readValue(row.getConfigValue(), type);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void write(String key, Object value, Integer userId) {
        try {
            String json = objectMapper.writeValueAsString(value);
            SystemConfig row = configMapper.selectById(key);
            if (row == null) {
                row = new SystemConfig();
                row.setConfigKey(key);
                row.setConfigValue(json);
                row.setUpdatedBy(userId);
                configMapper.insert(row);
            } else {
                row.setConfigValue(json);
                row.setUpdatedBy(userId);
                configMapper.updateById(row);
            }
        } catch (Exception e) {
            throw new BusinessException(500, "保存配置失败");
        }
    }

    private AuthSettingsDto defaultAuth() {
        AuthSettingsDto dto = new AuthSettingsDto();
        dto.setLocalLoginEnabled(true);
        dto.setDefaultGroupId(6);
        return dto;
    }

    private NotificationSettingsDto defaultNotification() {
        return new NotificationSettingsDto();
    }

    private RetentionSettingsDto defaultRetention() {
        return new RetentionSettingsDto();
    }

    private StorageSettingsDto defaultStorage() {
        return new StorageSettingsDto();
    }
}
