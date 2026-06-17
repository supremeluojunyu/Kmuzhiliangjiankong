package com.uqm.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析客户端真实 IP，兼容 FRP / Nginx / Vite 反向代理等多层隧道。
 * 优先从 X-Forwarded-For 链中取第一个公网地址；若均为内网则回退到最近一跳。
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

        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            String fromChain = pickFromForwardedChain(xff);
            if (StringUtils.hasText(fromChain)) {
                return fromChain;
            }
        }

        for (String header : new String[]{"X-Real-IP", "CF-Connecting-IP", "True-Client-IP"}) {
            String value = normalizeIp(request.getHeader(header));
            if (isUsableIp(value)) {
                return value;
            }
        }

        String forwarded = request.getHeader("Forwarded");
        if (StringUtils.hasText(forwarded)) {
            Matcher matcher = FORWARDED_FOR_PATTERN.matcher(forwarded);
            if (matcher.find()) {
                String value = normalizeIp(matcher.group(1));
                if (isUsableIp(value)) {
                    return value;
                }
            }
        }

        return normalizeIp(request.getRemoteAddr());
    }

    private static String pickFromForwardedChain(String xff) {
        String[] hops = xff.split(",");
        String firstHop = null;
        for (String hop : hops) {
            String ip = normalizeIp(hop.trim());
            if (!isUsableIp(ip)) {
                continue;
            }
            if (firstHop == null) {
                firstHop = ip;
            }
            if (isPublicIp(ip)) {
                return ip;
            }
        }
        return firstHop;
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

    private static boolean isPublicIp(String ip) {
        if (!isUsableIp(ip)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            return !(address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress());
        } catch (Exception e) {
            return false;
        }
    }
}
