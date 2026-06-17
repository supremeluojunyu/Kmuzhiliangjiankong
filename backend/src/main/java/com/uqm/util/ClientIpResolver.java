package com.uqm.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析客户端真实 IP，兼容 FRP（Proxy Protocol / X-Forwarded-For）/ Nginx / Vite 等多层代理。
 * <p>
 * X-Forwarded-For 约定：最左侧为原始客户端 IP（各层代理向右追加）。
 */
public final class ClientIpResolver {

    private static final Pattern FORWARDED_FOR_PATTERN =
            Pattern.compile("for=\"?([^;,\"\\s]+)\"?", Pattern.CASE_INSENSITIVE);

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 1. X-Forwarded-For 最左端 = 真实客户端（FRP HTTP 模式 / Nginx 标准写法）
        String xff = header(request, "X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            String leftmost = leftmostForwardedFor(xff);
            if (isUsableIp(leftmost)) {
                return leftmost;
            }
        }

        // 2. 边缘代理常用的单 IP 头（FRP + Proxy Protocol 经 Nginx 解析后写入）
        for (String name : new String[]{"X-Real-IP", "CF-Connecting-IP", "True-Client-IP", "X-Original-Forwarded-For"}) {
            String value = normalizeIp(header(request, name));
            if (isUsableIp(value) && !isLoopback(value)) {
                return value;
            }
        }

        // 3. RFC 7239 Forwarded: for=
        String forwarded = header(request, "Forwarded");
        if (StringUtils.hasText(forwarded)) {
            Matcher matcher = FORWARDED_FOR_PATTERN.matcher(forwarded);
            if (matcher.find()) {
                String value = normalizeIp(matcher.group(1));
                if (isUsableIp(value) && !isLoopback(value)) {
                    return value;
                }
            }
        }

        // 4. Spring/Tomcat RemoteIpValve 处理后的 remoteAddr
        String remote = normalizeIp(request.getRemoteAddr());
        if (isUsableIp(remote) && !isLoopback(remote)) {
            return remote;
        }

        // 5. 仅有内网/回环时仍返回 XFF 最左端（ campus 内网 IP 等）
        String xffLeft = StringUtils.hasText(xff) ? leftmostForwardedFor(xff) : null;
        if (isUsableIp(xffLeft)) {
            return xffLeft;
        }
        if (isUsableIp(normalizeIp(header(request, "X-Real-IP")))) {
            return normalizeIp(header(request, "X-Real-IP"));
        }
        return remote;
    }

    private static String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        int comma = value.indexOf(',');
        return comma >= 0 ? value.substring(0, comma).trim() : value.trim();
    }

    private static String leftmostForwardedFor(String xff) {
        if (!StringUtils.hasText(xff)) {
            return null;
        }
        for (String hop : xff.split(",")) {
            String ip = normalizeIp(hop.trim());
            if (isUsableIp(ip)) {
                return ip;
            }
        }
        return null;
    }

    private static String normalizeIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return null;
        }
        ip = ip.trim();
        if ("unknown".equalsIgnoreCase(ip)) {
            return null;
        }
        if (ip.startsWith("\"") && ip.endsWith("\"")) {
            ip = ip.substring(1, ip.length() - 1);
        }
        if (ip.startsWith("[")) {
            int end = ip.indexOf(']');
            if (end > 0) {
                ip = ip.substring(1, end);
            }
        }
        if (ip.regionMatches(true, 0, "::ffff:", 0, 7)) {
            ip = ip.substring(7);
        }
        return ip;
    }

    private static boolean isUsableIp(String ip) {
        return StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip);
    }

    private static boolean isLoopback(String ip) {
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip);
    }
}
