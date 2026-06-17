package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uqm.common.BusinessException;
import com.uqm.common.PageResult;
import com.uqm.dto.MessageRow;
import com.uqm.dto.MessageSendTargetUserVo;
import com.uqm.dto.MessageSendTargetVo;
import com.uqm.dto.MessageVo;
import com.uqm.dto.SendMessageRequest;
import com.uqm.entity.Message;
import com.uqm.entity.MessageReadStatus;
import com.uqm.entity.MessageTargetGroup;
import com.uqm.entity.MessageTargetUser;
import com.uqm.entity.User;
import com.uqm.entity.UserGroupEntity;
import com.uqm.mapper.MessageMapper;
import com.uqm.mapper.MessageReadStatusMapper;
import com.uqm.mapper.MessageTargetGroupMapper;
import com.uqm.mapper.MessageTargetUserMapper;
import com.uqm.mapper.UserGroupQueryMapper;
import com.uqm.mapper.UserMapper;
import com.uqm.security.LoginUser;
import com.uqm.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final MessageTargetGroupMapper targetGroupMapper;
    private final MessageTargetUserMapper targetUserMapper;
    private final MessageReadStatusMapper readStatusMapper;
    private final PermissionService permissionService;
    private final HtmlSanitizer htmlSanitizer;
    private final NotificationService notificationService;
    private final SystemConfigService systemConfigService;
    private final UserGroupQueryMapper userGroupQueryMapper;
    private final UserMapper userMapper;
    private final GroupService groupService;
    private final OperationLogService operationLogService;

    public PageResult<MessageVo> listMessages(LoginUser user, long page, long pageSize, String direction) {
        String dir = normalizeDirection(direction);
        long offset = (page - 1) * pageSize;
        List<MessageRow> rows = messageMapper.listAllForUser(
                user.getUserId(), dir, offset, pageSize);
        long total = messageMapper.countAllForUser(user.getUserId(), dir);
        List<MessageVo> list = rows.stream().map(row -> toVo(row, user.getUserId())).toList();
        return new PageResult<>(list, total, page, pageSize);
    }

    public long getUnreadCount(LoginUser user) {
        if (user.getCurrentGroupId() != null) {
            return messageMapper.countUnreadByGroup(user.getUserId(), user.getCurrentGroupId());
        }
        return messageMapper.countUnreadAllGroups(user.getUserId());
    }

    public List<MessageSendTargetVo> listSendTargets(LoginUser user) {
        permissionService.requirePermission(user, "message:send");
        List<MessageSendTargetVo> result = new ArrayList<>();
        for (UserGroupEntity group : groupService.listAll()) {
            List<MessageSendTargetUserVo> users = userGroupQueryMapper
                    .listUsersInGroup(group.getGroupId(), null)
                    .stream()
                    .map(u -> MessageSendTargetUserVo.builder()
                            .userId(u.getUserId())
                            .name(u.getName())
                            .account(u.getAccount())
                            .build())
                    .toList();
            result.add(MessageSendTargetVo.builder()
                    .groupId(group.getGroupId())
                    .groupName(group.getGroupName())
                    .users(users)
                    .build());
        }
        return result;
    }

    @Transactional
    public MessageVo sendMessage(LoginUser user, SendMessageRequest request) {
        permissionService.requirePermission(user, "message:send");

        List<Integer> groupIds = request.getTargetGroupIds() != null
                ? request.getTargetGroupIds().stream().distinct().toList()
                : List.of();
        List<Integer> userIds = request.getTargetUserIds() != null
                ? request.getTargetUserIds().stream().distinct().toList()
                : List.of();

        if (groupIds.isEmpty() && userIds.isEmpty()) {
            throw new BusinessException(400, "至少选择一个接收对象（组或个人）");
        }

        Message message = new Message();
        message.setSenderId(user.getUserId());
        message.setTitle(request.getTitle());
        message.setContent(htmlSanitizer.sanitize(request.getContent()));
        message.setMessageType(request.getMessageType() != null ? request.getMessageType() : "broadcast");
        message.setTaskId(request.getTaskId());
        message.setInstanceId(request.getInstanceId());
        message.setSendTime(LocalDateTime.now());
        messageMapper.insert(message);

        if (!groupIds.isEmpty()) {
            targetGroupMapper.batchInsert(message.getMessageId(), groupIds);
        }
        if (!userIds.isEmpty()) {
            targetUserMapper.batchInsert(message.getMessageId(), userIds);
        }

        notifyTargets(message.getTitle(), message.getContent(), groupIds, userIds);

        operationLogService.log(user, "message:send", "message", message.getMessageId(),
                Map.of("title", message.getTitle(), "targetGroupIds", groupIds, "targetUserIds", userIds));

        MessageVo vo = toVoFromEntity(message);
        vo.setSentByMe(true);
        vo.setIsRead(true);
        vo.setTargetGroupNames(messageMapper.listTargetGroupNames(message.getMessageId()));
        vo.setTargetUserNames(messageMapper.listTargetUserNames(message.getMessageId()));
        return vo;
    }

    @Transactional
    public void deleteMessage(LoginUser user, Integer messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(404, "消息不存在");
        }
        if (!canDeleteMessage(user, message)) {
            throw new BusinessException(403, "无权删除该消息");
        }

        readStatusMapper.delete(new LambdaQueryWrapper<MessageReadStatus>()
                .eq(MessageReadStatus::getMessageId, messageId));
        targetGroupMapper.delete(new LambdaQueryWrapper<MessageTargetGroup>()
                .eq(MessageTargetGroup::getMessageId, messageId));
        targetUserMapper.delete(new LambdaQueryWrapper<MessageTargetUser>()
                .eq(MessageTargetUser::getMessageId, messageId));
        messageMapper.deleteById(messageId);

        operationLogService.log(user, "message:delete", "message", messageId,
                Map.of("title", message.getTitle(), "messageType", message.getMessageType()));
    }

    private boolean canDeleteMessage(LoginUser user, Message message) {
        if (isMessageAdmin(user)) {
            return true;
        }
        return permissionService.hasPermission(user, "message:send")
                && user.getUserId().equals(message.getSenderId());
    }

    private boolean isMessageAdmin(LoginUser user) {
        return permissionService.hasPermission(user, "user:manage")
                || permissionService.hasPermission(user, "group:manage")
                || permissionService.hasPermission(user, "system:config");
    }

    @Transactional
    public void markRead(LoginUser user, Integer messageId) {
        verifyMessageAccess(user.getUserId(), messageId);
        readStatusMapper.markRead(messageId, user.getUserId());
    }

    @Transactional
    public void markAllRead(LoginUser user) {
        List<MessageRow> rows = messageMapper.listAllForUser(user.getUserId(), "received", 0, 1000);
        List<Integer> unreadIds = rows.stream()
                .filter(r -> r.getIsRead() == null || r.getIsRead() == 0)
                .map(MessageRow::getMessageId)
                .toList();
        if (!unreadIds.isEmpty()) {
            readStatusMapper.markReadBatch(unreadIds, user.getUserId());
        }
    }

    private String normalizeDirection(String direction) {
        if ("sent".equals(direction) || "received".equals(direction)) {
            return direction;
        }
        return "all";
    }

    private void verifyMessageAccess(Integer userId, Integer messageId) {
        if (messageMapper.userCanAccess(userId, messageId) == 0) {
            throw new BusinessException(403, "无权访问该消息");
        }
    }

    private MessageVo toVo(MessageRow row, Integer currentUserId) {
        boolean sentByMe = currentUserId != null && currentUserId.equals(row.getSenderId());
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
                .isRead(sentByMe || (row.getIsRead() != null && row.getIsRead() == 1))
                .sentByMe(sentByMe)
                .build();
        vo.setTargetGroupNames(messageMapper.listTargetGroupNames(row.getMessageId()));
        vo.setTargetUserNames(messageMapper.listTargetUserNames(row.getMessageId()));
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

    private void notifyTargets(String title, String content, List<Integer> groupIds, List<Integer> userIds) {
        if (!systemConfigService.getNotification().isNotifyOnMessageBroadcast()) {
            return;
        }
        Set<Integer> notified = new HashSet<>();
        for (Integer groupId : groupIds) {
            userGroupQueryMapper.listUsersInGroup(groupId, null).forEach(item -> {
                notifyUser(notified, item.getUserId(), title, content);
            });
        }
        for (Integer userId : userIds) {
            notifyUser(notified, userId, title, content);
        }
    }

    private void notifyUser(Set<Integer> notified, Integer userId, String title, String content) {
        if (!notified.add(userId)) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        notificationService.notifyBroadcast(title, content, user.getEmail(), user.getWechatUserId());
    }
}
