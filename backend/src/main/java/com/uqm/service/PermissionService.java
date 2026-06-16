package com.uqm.service;

import com.uqm.common.BusinessException;
import com.uqm.mapper.PermissionMapper;
import com.uqm.mapper.UserGroupRelationMapper;
import com.uqm.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper permissionMapper;
    private final UserGroupRelationMapper userGroupRelationMapper;

    public void requirePermission(LoginUser user, String permissionCode) {
        if (user.getCurrentGroupId() == null) {
            throw new BusinessException(403, "未选择身份组");
        }
        List<String> codes = permissionMapper.listCodesByGroupId(user.getCurrentGroupId());
        if (!codes.contains(permissionCode)) {
            throw new BusinessException(403, "无权限：" + permissionCode);
        }
    }

    public boolean hasPermission(LoginUser user, String permissionCode) {
        if (user.getCurrentGroupId() == null) {
            return false;
        }
        return permissionMapper.listCodesByGroupId(user.getCurrentGroupId()).contains(permissionCode);
    }
}
