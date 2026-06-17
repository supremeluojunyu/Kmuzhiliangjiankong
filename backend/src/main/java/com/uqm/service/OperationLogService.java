package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uqm.common.BusinessException;
import com.uqm.common.PageResult;
import com.uqm.dto.OperationLogVo;
import com.uqm.entity.OperationLog;
import com.uqm.entity.User;
import com.uqm.entity.UserGroupEntity;
import com.uqm.mapper.GroupMapper;
import com.uqm.mapper.OperationLogMapper;
import com.uqm.mapper.UserMapper;
import com.uqm.security.LoginUser;
import com.uqm.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper logMapper;
    private final UserMapper userMapper;
    private final GroupMapper groupMapper;
    private final ObjectMapper objectMapper;
    private final PermissionService permissionService;

    public void log(LoginUser user, String action, String targetType, Integer targetId, Object detail) {
        OperationLog log = new OperationLog();
        log.setUserId(user != null ? user.getUserId() : null);
        log.setGroupId(user != null ? user.getCurrentGroupId() : null);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setIp(resolveIp());
        log.setCreatedAt(LocalDateTime.now());
        if (detail != null) {
            try {
                log.setDetailJson(objectMapper.writeValueAsString(detail));
            } catch (Exception e) {
                log.setDetailJson(String.valueOf(detail));
            }
        }
        logMapper.insert(log);
    }

    public PageResult<OperationLogVo> list(
            LoginUser user,
            long page,
            long pageSize,
            String action,
            String userName,
            LocalDateTime dateFrom,
            LocalDateTime dateTo) {
        requireLogViewPermission(user);

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreatedAt);
        if (StringUtils.hasText(action)) {
            wrapper.like(OperationLog::getAction, action.trim());
        }
        if (StringUtils.hasText(userName)) {
            wrapper.apply("user_id IN (SELECT user_id FROM user WHERE name LIKE {0})",
                    "%" + userName.trim() + "%");
        }
        if (dateFrom != null) {
            wrapper.ge(OperationLog::getCreatedAt, dateFrom);
        }
        if (dateTo != null) {
            wrapper.le(OperationLog::getCreatedAt, dateTo);
        }

        Page<OperationLog> result = logMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new PageResult<>(
                result.getRecords().stream().map(this::toVo).toList(),
                result.getTotal(),
                page,
                pageSize
        );
    }

    public List<String> listActionTypes(LoginUser user) {
        requireLogViewPermission(user);
        return logMapper.listDistinctActions();
    }

    private void requireLogViewPermission(LoginUser user) {
        if (permissionService.hasPermission(user, "user:manage")
                || permissionService.hasPermission(user, "group:manage")
                || permissionService.hasPermission(user, "system:config")) {
            return;
        }
        throw new BusinessException(403, "无权限查看操作日志");
    }

    private OperationLogVo toVo(OperationLog log) {
        String userName = null;
        if (log.getUserId() != null) {
            User u = userMapper.selectById(log.getUserId());
            userName = u != null ? u.getName() : null;
        }
        String groupName = null;
        if (log.getGroupId() != null) {
            UserGroupEntity group = groupMapper.selectById(log.getGroupId());
            groupName = group != null ? group.getGroupName() : null;
        }
        return OperationLogVo.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .userName(userName)
                .groupId(log.getGroupId())
                .groupName(groupName)
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .detailJson(log.getDetailJson())
                .ip(log.getIp())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String resolveIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                return ClientIpResolver.resolve(request);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
