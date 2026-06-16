package com.uqm.controller;

import com.uqm.common.ApiResponse;
import com.uqm.dto.LoginRequest;
import com.uqm.dto.LoginResponse;
import com.uqm.dto.PublicAuthConfigVo;
import com.uqm.service.AuthService;
import com.uqm.service.ExternalAuthService;
import com.uqm.service.SystemConfigService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SystemConfigService systemConfigService;
    private final ExternalAuthService externalAuthService;

    @GetMapping("/config")
    public ApiResponse<PublicAuthConfigVo> publicConfig() {
        return ApiResponse.ok(systemConfigService.getPublicAuthConfig());
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/cas/login")
    public void casLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect(externalAuthService.buildCasLoginRedirect());
    }

    @GetMapping("/cas/callback")
    public void casCallback(@RequestParam(required = false) String ticket, HttpServletResponse response) throws IOException {
        LoginResponse login = externalAuthService.handleCasCallback(ticket);
        redirectWithToken(response, login);
    }

    @GetMapping("/oauth2/login")
    public void oauthLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect(externalAuthService.buildOAuthLoginRedirect());
    }

    @GetMapping("/oauth2/callback")
    public void oauthCallback(@RequestParam(required = false) String code, HttpServletResponse response) throws IOException {
        LoginResponse login = externalAuthService.handleOAuthCallback(code);
        redirectWithToken(response, login);
    }

    private void redirectWithToken(HttpServletResponse response, LoginResponse login) throws IOException {
        String base = systemConfigService.getAuth().getFrontendBaseUrl();
        if (!StringUtils.hasText(base)) {
            base = "http://localhost:5173";
        }
        String url = base.replaceAll("/$", "") + "/login/callback?token="
                + URLEncoder.encode(login.getToken(), StandardCharsets.UTF_8);
        response.sendRedirect(url);
    }
}
