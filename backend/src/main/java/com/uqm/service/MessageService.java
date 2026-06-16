package com.uqm.service;

import com.uqm.common.BusinessException;
import com.uqm.common.PageResult;
import com.uqm.dto.MessageRow;
import com.uqm.dto.MessageVo;
import com.uqm.dto.SendMessageRequest;
import com.uqm.entity.Message;
import com.uqm.mapper.MessageMapper;
import com.uqm.mapper.MessageReadStatusMapper;
import com.uqm.mapper.MessageTargetGroupMapper;
import com.uqm.mapper.UserGroupQueryMapper;
import com.uqm.mapper.UserMapper;
import com.uqm.entity.User;
import com.uqm.security.LoginUser;
import com.uqm.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final MessageTargetGroupMapper targetGroupMapper;
    private final MessageReadStatusMapper readStatusMapper;
    private final PermissionService permissionService;
    private final HtmlSanitizer htmlSanitizer;
    private final NotificationService notificationService;
    private final SystemConfigService systemConfigService;
    private final UserGroupQueryMapper userGroupQueryMapper;
    private final UserMapper userMapper;

    public PageResult<MessageVo> listMessages(LoginUser user, long page, long pageSize) {
        long offset = (page - 1) * pageSize;
        List<MessageRow> rows = messageMapper.listAllForUser(
                user.getUserId(), offset, pageSize);
        long total = messageMapper.countAllForUser(user.getUserId());
        List<MessageVo> list = rows.stream().map(this::toVo).toList();
        return new PageResult<>(list, total, page, pageSize);
    }

    public long getUnreadCount(LoginUser user) {
        if (user.getCurrentGroupId() != null) {
            return messageMapper.countUnreadByGroup(user.getUserId(), user.getCurrentGroupId());
        }
        return messageMapper.countUnreadAllGroups(user.getUserId());
    }

    @Transactional
    public MessageVo sendMessage(LoginUser user, SendMessageRequest request) {
        permissionService.requirePermission(user, "message:send");

        Message message = new Message();
        message.setSenderId(user.getUserId());
        message.setTitle(request.getTitle());
        message.setContent(htmlSanitizer.sanitize(request.getContent()));
        message.setMessageType(request.getMessageType() != null ? request.getMessageType() : "broadcast");
        message.setTaskId(request.getTaskId());
        message.setInstanceId(request.getInstanceId());
        message.setSendTime(LocalDateTime.now());
        messageMapper.insert(message);

        targetGroupMapper.batchInsert(message.getMessageId(), request.getTargetGroupIds());

        notifyBroadcastTargets(message.getTitle(), message.getContent(), request.getTargetGroupIds());

        MessageVo vo = toVoFromEntity(message);
        vo.setTargetGroupNames(messageMapper.listTargetGroupNames(message.getMessageId()));
        return vo;
    }

    @Transactional
    public void markRead(LoginUser user, Integer messageId) {
        verifyMessageAccess(user.getUserId(), messageId);
        readStatusMapper.markRead(messageId, user.getUserId());
    }

    @Transactional
    public void markAllRead(LoginUser user) {
        List<MessageRow> rows = messageMapper.listAllForUser(user.getUserId(), 0, 1000);
        List<Integer> unreadIds = rows.stream()
                .filter(r -> r.getIsRead() == null || r.getIsRead() == 0)
                .map(MessageRow::getMessageId)
                .toList();
        if (!unreadIds.isEmpty()) {
            readStatusMapper.markReadBatch(unreadIds, user.getUserId());
        }
    }

    private void verifyMessageAccess(Integer userId, Integer messageId) {
        if (messageMapper.userCanAccess(userId, messageId) == 0) {
            throw new BusinessException(403, "无权访问该消息");
        }
    }

    private MessageVo toVo(MessageRow row) {
        MessageVo vo = MessageVo.builder()
                .messageId(row.getMessageId())
                .senderId(row.getSenderId())
                .senderName(row.getSenderName())
                .title(row.getTitle())
                .content(row.getContent())
                .messageType(row.getMessageType())
                .taskId(row.getTaskId())
                .instanceId(row.getInstanceId())
                .sendTime(row.getSendTime())
                .isRead(row.getIsRead() != null && row.getIsRead() == 1)
                .build();
        vo.setTargetGroupNames(messageMapper.listTargetGroupNames(row.getMessageId()));
        return vo;
    }

    private MessageVo toVoFromEntity(Message message) {
        return MessageVo.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSenderId())
                .title(message.getTitle())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .taskId(message.getTaskId())
                .instanceId(message.getInstanceId())
                .sendTime(message.getSendTime())
                .isRead(false)
                .build();
    }

    private void notifyBroadcastTargets(String title, String content, List<Integer> groupIds) {
        if (!systemConfigService.getNotification().isNotifyOnMessageBroadcast()) {
            return;
        }
        java.util.Set<Integer> notified = new java.util.HashSet<>();
        for (Integer groupId : groupIds) {
            userGroupQueryMapper.listUsersInGroup(groupId, null).forEach(item -> {
                if (!notified.add(item.getUserId())) {
                    return;
                }
                User user = userMapper.selectById(item.getUserId());
                if (user == null) {
                    return;
                }
                notificationService.notifyBroadcast(title, content, user.getEmail(), user.getWechatUserId());
            });
        }
    }
}
