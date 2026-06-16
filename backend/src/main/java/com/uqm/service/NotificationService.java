package com.uqm.service;

import com.uqm.dto.NotificationSettingsDto;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String NOTIFY_PREFIX = "【昆明学院质控】";

    private final SystemConfigService systemConfigService;
    private final RestClient restClient = RestClient.create();

    public void notifyTaskPublished(String taskName, String recipientEmail, String recipientWechatUserId) {
        NotificationSettingsDto cfg = systemConfigService.getNotification();
        if (!cfg.isNotifyOnTaskPublish()) {
            return;
        }
        String subject = NOTIFY_PREFIX + "新任务发布：" + taskName;
        String body = "任务「" + taskName + "」已发布，请登录系统查看并处理。";
        sendEmail(cfg, recipientEmail, subject, body);
        sendWechat(cfg, recipientWechatUserId, subject + "\n" + body);
    }

    public void notifyBroadcast(String title, String content, String recipientEmail, String recipientWechatUserId) {
        NotificationSettingsDto cfg = systemConfigService.getNotification();
        if (!cfg.isNotifyOnMessageBroadcast()) {
            return;
        }
        sendEmail(cfg, recipientEmail, NOTIFY_PREFIX + title, content);
        sendWechat(cfg, recipientWechatUserId, title + "\n" + stripHtml(content));
    }

    public void notifyDeadline(String title, String body, String recipientEmail, String recipientWechatUserId) {
        NotificationSettingsDto cfg = systemConfigService.getNotification();
        if (!cfg.isNotifyOnDeadline()) {
            return;
        }
        sendEmail(cfg, recipientEmail, title, body);
        sendWechat(cfg, recipientWechatUserId, title + "\n" + body);
    }

    public void testNotification(String channel, String target) {
        NotificationSettingsDto cfg = systemConfigService.getNotification();
        if ("email".equals(channel)) {
            sendEmail(cfg, target, NOTIFY_PREFIX + "测试邮件", "这是一封来自昆明学院质量监控任务管理系统的测试邮件。");
        } else if ("wechat".equals(channel)) {
            sendWechat(cfg, target, NOTIFY_PREFIX + "测试消息\n这是一条测试企业微信通知。");
        }
    }

    private void sendEmail(NotificationSettingsDto cfg, String to, String subject, String text) {
        if (!cfg.isEmailEnabled() || !StringUtils.hasText(to) || !StringUtils.hasText(cfg.getSmtpHost())) {
            return;
        }
        try {
            JavaMailSenderImpl sender = buildMailSender(cfg);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(StringUtils.hasText(cfg.getSmtpFrom()) ? cfg.getSmtpFrom() : cfg.getSmtpUsername());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            sender.send(message);
        } catch (Exception e) {
            log.warn("发送邮件失败: {}", e.getMessage());
        }
    }

    private void sendWechat(NotificationSettingsDto cfg, String userId, String content) {
        if (!cfg.isWechatEnabled() || !StringUtils.hasText(userId)
                || !StringUtils.hasText(cfg.getWechatCorpId()) || !StringUtils.hasText(cfg.getWechatSecret())) {
            return;
        }
        try {
            String tokenUrl = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid="
                    + cfg.getWechatCorpId() + "&corpsecret=" + cfg.getWechatSecret();
            Map<?, ?> tokenResp = restClient.get().uri(tokenUrl).retrieve().body(Map.class);
            if (tokenResp == null || tokenResp.get("access_token") == null) {
                log.warn("企业微信 token 获取失败");
                return;
            }
            String token = String.valueOf(tokenResp.get("access_token"));
            Map<String, Object> body = Map.of(
                    "touser", userId,
                    "msgtype", "text",
                    "agentid", Integer.parseInt(cfg.getWechatAgentId()),
                    "text", Map.of("content", content)
            );
            restClient.post()
                    .uri("https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=" + token)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("发送企业微信失败: {}", e.getMessage());
        }
    }

    private JavaMailSenderImpl buildMailSender(NotificationSettingsDto cfg) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(cfg.getSmtpHost());
        sender.setPort(cfg.getSmtpPort());
        sender.setUsername(cfg.getSmtpUsername());
        sender.setPassword(cfg.getSmtpPassword());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(cfg.isSmtpSsl()));
        return sender;
    }

    private String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]+>", "").trim();
    }
}
