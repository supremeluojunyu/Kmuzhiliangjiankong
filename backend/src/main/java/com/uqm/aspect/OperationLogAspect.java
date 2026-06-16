package com.uqm.aspect;

import com.uqm.common.LogOperation;
import com.uqm.security.LoginUser;
import com.uqm.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    @AfterReturning("@annotation(logOperation)")
    public void after(JoinPoint joinPoint, LogOperation logOperation) {
        LoginUser user = currentUser();
        if (user == null) {
            return;
        }
        Integer targetId = extractTargetId(joinPoint.getArgs());
        operationLogService.log(
                user,
                logOperation.action(),
                logOperation.targetType(),
                targetId,
                null
        );
    }

    private LoginUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    private Integer extractTargetId(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Integer i) {
                return i;
            }
        }
        return null;
    }
}
