package com.uqm.service;

import com.uqm.mapper.UserGroupRelationMapper;
import com.uqm.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 系统管理员删除策略：除不能删自己、不能删最后一名系统管理员、不能删系统管理员组外，
 * 系统管理员可强制级联删除用户/组及其全部关联任务（含进行中）。
 */
@Service
@RequiredArgsConstructor
public class AdminGuardService {

    public static final int SYSTEM_ADMIN_GROUP_ID = 1;

    private final PermissionService permissionService;
    private final UserGroupRelationMapper userGroupRelationMapper;
    private final ResourceCascadeService resourceCascadeService;

    public boolean isSystemAdministrator(LoginUser user) {
        if (user == null) {
            return false;
        }
        if (permissionService.hasPermission(user, "system:config")) {
            return true;
        }
        return user.getCurrentGroupId() != null
                && user.getCurrentGroupId() == SYSTEM_ADMIN_GROUP_ID;
    }

    public boolean isSystemAdminMember(Integer userId) {
        return userGroupRelationMapper.countUserInGroup(userId, SYSTEM_ADMIN_GROUP_ID) > 0;
    }

    public boolean isLastSystemAdmin(Integer userId) {
        if (!isSystemAdminMember(userId)) {
            return false;
        }
        return userGroupRelationMapper.countMembers(SYSTEM_ADMIN_GROUP_ID) <= 1;
    }

    public boolean canDeleteUser(LoginUser operator, Integer targetUserId) {
        if (operator.getUserId().equals(targetUserId)) {
            return false;
        }
        if (isLastSystemAdmin(targetUserId)) {
            return false;
        }
        if (isSystemAdministrator(operator)) {
            return true;
        }
        return resourceCascadeService.isUserDeletable(targetUserId);
    }

    public boolean canDeleteGroup(LoginUser operator, Integer groupId) {
        if (groupId == SYSTEM_ADMIN_GROUP_ID) {
            return false;
        }
        if (isSystemAdministrator(operator)) {
            return true;
        }
        return resourceCascadeService.isGroupDeletable(groupId);
    }

    public boolean canForceCascadeDelete(LoginUser operator) {
        return isSystemAdministrator(operator);
    }
}
