package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uqm.common.BusinessException;
import com.uqm.dto.AuthSettingsDto;
import com.uqm.dto.LoginResponse;
import com.uqm.entity.User;
import com.uqm.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExternalAuthService {

    private final SystemConfigService systemConfigService;
    private final AuthService authService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public String buildCasLoginRedirect() {
        AuthSettingsDto auth = requireCas();
        String service = resolveServiceUrl(auth);
        String login = trimSlash(auth.getCasServerUrl()) + auth.getCasLoginPath();
        return login + "?service=" + URLEncoder.encode(service, StandardCharsets.UTF_8);
    }

    public LoginResponse handleCasCallback(String ticket) {
        AuthSettingsDto auth = requireCas();
        if (!StringUtils.hasText(ticket)) {
            throw new BusinessException(400, "缺少 CAS ticket");
        }
        String service = resolveServiceUrl(auth);
        String validateUrl = trimSlash(auth.getCasServerUrl()) + "/serviceValidate?ticket="
                + URLEncoder.encode(ticket, StandardCharsets.UTF_8)
                + "&service=" + URLEncoder.encode(service, StandardCharsets.UTF_8);
        String xml = restClient.get().uri(validateUrl).retrieve().body(String.class);
        String account = parseCasAccount(xml);
        if (!StringUtils.hasText(account)) {
            throw new BusinessException(401, "CAS 认证失败");
        }
        return authService.loginByExternalAccount(account, auth.isAutoProvision(), auth.getDefaultGroupId());
    }

    public String buildOAuthLoginRedirect() {
        AuthSettingsDto auth = requireOAuth();
        String state = UUID.randomUUID().toString();
        String redirect = auth.getOauthRedirectUri();
        if (!StringUtils.hasText(redirect)) {
            redirect = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/auth/oauth2/callback").build().toUriString();
        }
        return trimSlash(auth.getOauthIssuer()) + "/oauth2/authorize?response_type=code&client_id="
                + URLEncoder.encode(auth.getOauthClientId(), StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8)
                + "&scope=" + URLEncoder.encode(auth.getOauthScope(), StandardCharsets.UTF_8)
                + "&state=" + state;
    }

    public LoginResponse handleOAuthCallback(String code) {
        AuthSettingsDto auth = requireOAuth();
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(400, "缺少授权码");
        }
        String redirect = StringUtils.hasText(auth.getOauthRedirectUri())
                ? auth.getOauthRedirectUri()
                : ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/auth/oauth2/callback").build().toUriString();
        Map<?, ?> tokenResp = restClient.post()
                .uri(trimSlash(auth.getOauthIssuer()) + "/oauth2/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("grant_type=authorization_code&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8)
                        + "&client_id=" + URLEncoder.encode(auth.getOauthClientId(), StandardCharsets.UTF_8)
                        + "&client_secret=" + URLEncoder.encode(auth.getOauthClientSecret(), StandardCharsets.UTF_8))
                .retrieve()
                .body(Map.class);
        if (tokenResp == null || tokenResp.get("access_token") == null) {
            throw new BusinessException(401, "OAuth 令牌交换失败");
        }
        String accessToken = String.valueOf(tokenResp.get("access_token"));
        Map<?, ?> userInfo = restClient.get()
                .uri(trimSlash(auth.getOauthIssuer()) + "/oauth2/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);
        String account = extractOAuthAccount(userInfo);
        if (!StringUtils.hasText(account)) {
            throw new BusinessException(401, "无法获取 OAuth 用户标识");
        }
        return authService.loginByExternalAccount(account, auth.isAutoProvision(), auth.getDefaultGroupId());
    }

    private AuthSettingsDto requireCas() {
        AuthSettingsDto auth = systemConfigService.getAuth();
        if (!auth.isEnabled() || !"cas".equals(auth.getProvider())) {
            throw new BusinessException(400, "CAS 未启用");
        }
        return auth;
    }

    private AuthSettingsDto requireOAuth() {
        AuthSettingsDto auth = systemConfigService.getAuth();
        if (!auth.isEnabled() || !"oauth2".equals(auth.getProvider())) {
            throw new BusinessException(400, "OAuth2 未启用");
        }
        return auth;
    }

    private String resolveServiceUrl(AuthSettingsDto auth) {
        if (StringUtils.hasText(auth.getServiceUrl())) {
            return auth.getServiceUrl();
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/auth/cas/callback").build().toUriString();
    }

    private String parseCasAccount(String xml) {
        if (xml == null) {
            return null;
        }
        int start = xml.indexOf("<cas:user>");
        if (start < 0) {
            start = xml.indexOf(":user>");
        }
        if (start < 0) {
            return null;
        }
        int end = xml.indexOf("</", start);
        if (end < 0) {
            return null;
        }
        String tag = xml.substring(start, end);
        int gt = tag.lastIndexOf('>');
        return gt >= 0 ? tag.substring(gt + 1) : null;
    }

    private String extractOAuthAccount(Map<?, ?> userInfo) {
        if (userInfo == null) {
            return null;
        }
        Object preferred = userInfo.get("preferred_username");
        if (preferred != null) {
            return String.valueOf(preferred);
        }
        Object sub = userInfo.get("sub");
        if (sub != null) {
            return String.valueOf(sub);
        }
        Object email = userInfo.get("email");
        return email != null ? String.valueOf(email) : null;
    }

    private String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
