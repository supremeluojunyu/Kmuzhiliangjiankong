package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicAuthConfigVo {
    private boolean externalAuthEnabled;
    private String provider;
    private boolean localLoginEnabled;
    private String casLoginUrl;
    private String oauthLoginUrl;
}
