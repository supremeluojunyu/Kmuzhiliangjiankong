package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uqm.common.PageResult;
import com.uqm.dto.OperationLogVo;
import com.uqm.entity.OperationLog;
import com.uqm.entity.User;
import com.uqm.mapper.OperationLogMapper;
import com.uqm.mapper.UserMapper;
import com.uqm.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper logMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

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

    public PageResult<OperationLogVo> list(LoginUser user, long page, long pageSize, String action) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreatedAt);
        if (StringUtils.hasText(action)) {
            wrapper.like(OperationLog::getAction, action);
        }
        Page<OperationLog> result = logMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new PageResult<>(
                result.getRecords().stream().map(this::toVo).toList(),
                result.getTotal(),
                page,
                pageSize
        );
    }

    private OperationLogVo toVo(OperationLog log) {
        String userName = null;
        if (log.getUserId() != null) {
            User u = userMapper.selectById(log.getUserId());
            userName = u != null ? u.getName() : null;
        }
        return OperationLogVo.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .userName(userName)
                .groupId(log.getGroupId())
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
                String xff = request.getHeader("X-Forwarded-For");
                if (StringUtils.hasText(xff)) {
                    return xff.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
