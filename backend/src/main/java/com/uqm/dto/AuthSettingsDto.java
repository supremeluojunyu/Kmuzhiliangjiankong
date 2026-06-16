package com.uqm.dto;

import lombok.Data;

@Data
public class AuthSettingsDto {
    private boolean enabled;
    /** local | cas | oauth2 */
    private String provider = "local";
    private boolean localLoginEnabled = true;
    private String casServerUrl;
    private String casLoginPath = "/login";
    private String serviceUrl;
    private String oauthIssuer;
    private String oauthClientId;
    private String oauthClientSecret;
    private String oauthRedirectUri;
    private String oauthScope = "openid profile email";
    private boolean autoProvision;
    private Integer defaultGroupId = 6;
    private String frontendBaseUrl = "http://localhost:5173";
}
