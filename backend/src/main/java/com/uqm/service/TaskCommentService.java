package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uqm.common.BusinessException;
import com.uqm.dto.FlowNode;
import com.uqm.dto.MessageVo;
import com.uqm.dto.TaskFlowConfig;
import com.uqm.entity.Message;
import com.uqm.entity.TaskDefinition;
import com.uqm.entity.TaskInstance;
import com.uqm.entity.UserGroupRelation;
import com.uqm.mapper.MessageMapper;
import com.uqm.mapper.MessageTargetGroupMapper;
import com.uqm.mapper.TaskDefinitionMapper;
import com.uqm.mapper.TaskInstanceMapper;
import com.uqm.mapper.UserGroupRelationMapper;
import com.uqm.security.LoginUser;
import com.uqm.util.HtmlSanitizer;
import com.uqm.workflow.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskCommentService {

    private final TaskInstanceMapper instanceMapper;
    private final TaskDefinitionMapper taskMapper;
    private final MessageMapper messageMapper;
    private final MessageTargetGroupMapper targetGroupMapper;
    private final UserGroupRelationMapper userGroupRelationMapper;
    private final WorkflowEngine workflowEngine;
    private final HtmlSanitizer htmlSanitizer;

    public List<MessageVo> listComments(LoginUser user, Integer instanceId) {
        verifyAccess(user, instanceId);
        return messageMapper.listCommentsForInstance(user.getUserId(), instanceId).stream()
                .map(row -> {
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
                })
                .toList();
    }

    @Transactional
    public MessageVo postComment(LoginUser user, Integer instanceId, String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(400, "评论内容不能为空");
        }
        TaskInstance instance = verifyAccess(user, instanceId);
        TaskDefinition task = taskMapper.selectById(instance.getTaskDefinitionId());
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        List<Integer> targetGroups = resolveTargetGroups(task, instance);

        Message message = new Message();
        message.setSenderId(user.getUserId());
        message.setTitle("任务讨论");
        message.setContent(htmlSanitizer.sanitize(content));
        message.setMessageType("comment");
        message.setTaskId(task.getTaskId());
        message.setInstanceId(instanceId);
        message.setSendTime(LocalDateTime.now());
        messageMapper.insert(message);
        targetGroupMapper.batchInsert(message.getMessageId(), targetGroups);

        MessageVo vo = MessageVo.builder()
                .messageId(message.getMessageId())
                .senderId(user.getUserId())
                .title(message.getTitle())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .taskId(message.getTaskId())
                .instanceId(message.getInstanceId())
                .sendTime(message.getSendTime())
                .isRead(true)
                .build();
        vo.setTargetGroupNames(messageMapper.listTargetGroupNames(message.getMessageId()));
        return vo;
    }

    private TaskInstance verifyAccess(LoginUser user, Integer instanceId) {
        TaskInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException(404, "任务实例不存在");
        }
        TaskDefinition task = taskMapper.selectById(instance.getTaskDefinitionId());
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        List<Integer> groups = resolveTargetGroups(task, instance);
        long member = userGroupRelationMapper.selectCount(new LambdaQueryWrapper<UserGroupRelation>()
                .eq(UserGroupRelation::getUserId, user.getUserId())
                .in(UserGroupRelation::getGroupId, groups));
        if (member == 0) {
            throw new BusinessException(403, "无权访问该任务讨论");
        }
        return instance;
    }

    private List<Integer> resolveTargetGroups(TaskDefinition task, TaskInstance instance) {
        Set<Integer> groupIds = new LinkedHashSet<>();
        groupIds.add(instance.getTargetGroupId());
        TaskFlowConfig config = workflowEngine.parseJson(task.getConfigJson());
        if (config.getNodes() != null) {
            for (FlowNode node : config.getNodes()) {
                if (node.getExecuteGroupId() != null) {
                    groupIds.add(node.getExecuteGroupId());
                }
            }
        }
        return new ArrayList<>(groupIds);
    }
}
