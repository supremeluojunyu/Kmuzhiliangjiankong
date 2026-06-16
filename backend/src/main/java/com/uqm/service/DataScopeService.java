package com.uqm.service;

import com.uqm.common.BusinessException;
import com.uqm.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据范围：校级（全校）与院级（本院）隔离。
 * 院级管理组（group_id=3）操作时自动限定为 user.college_id。
 */
@Service
@RequiredArgsConstructor
public class DataScopeService {

    public static final int COLLEGE_ADMIN_GROUP_ID = 3;

    private final PermissionService permissionService;

    /** 是否院级管理身份 */
    public boolean isCollegeScoped(LoginUser user) {
        return user.getCurrentGroupId() != null
                && user.getCurrentGroupId() == COLLEGE_ADMIN_GROUP_ID;
    }

    /**
     * 统计查询的学院过滤：null=全校，非 null=仅该学院。
     */
    public Integer statCollegeFilter(LoginUser user) {
        if (permissionService.hasPermission(user, "stat:view_all")) {
            return null;
        }
        if (permissionService.hasPermission(user, "stat:view_college")) {
            return requireCollegeId(user);
        }
        throw new BusinessException(403, "无查看统计权限");
    }

    public void requireStatAccess(LoginUser user) {
        if (!permissionService.hasPermission(user, "stat:view_all")
                && !permissionService.hasPermission(user, "stat:view_college")) {
            throw new BusinessException(403, "无查看统计权限");
        }
    }

    /** 变更类操作（分配、用户管理）的学院范围 */
    public Integer mutationCollegeFilter(LoginUser user) {
        if (isCollegeScoped(user)) {
            return requireCollegeId(user);
        }
        return null;
    }

    public void validateCollegeIds(LoginUser user, List<Integer> collegeIds) {
        Integer scope = mutationCollegeFilter(user);
        if (scope == null) {
            return;
        }
        if (collegeIds == null || collegeIds.size() != 1 || !scope.equals(collegeIds.get(0))) {
            throw new BusinessException(403, "院级管理员仅可操作本院数据");
        }
    }

    public void validateTargetCollege(LoginUser user, Integer collegeId) {
        Integer scope = mutationCollegeFilter(user);
        if (scope != null && (collegeId == null || !scope.equals(collegeId))) {
            throw new BusinessException(403, "院级管理员仅可操作本院用户");
        }
    }

    public void validateTargetUser(LoginUser user, Integer targetCollegeId) {
        validateTargetCollege(user, targetCollegeId);
    }

    /** 导出等场景：院级身份优先，否则按统计权限过滤 */
    public Integer resolveCollegeFilter(LoginUser user) {
        Integer scope = mutationCollegeFilter(user);
        if (scope != null) {
            return scope;
        }
        if (permissionService.hasPermission(user, "stat:view_college")
                && !permissionService.hasPermission(user, "stat:view_all")) {
            return requireCollegeId(user);
        }
        return null;
    }

    private Integer requireCollegeId(LoginUser user) {
        if (user.getCollegeId() == null) {
            throw new BusinessException(400, "当前用户未关联学院，无法使用院级数据范围");
        }
        return user.getCollegeId();
    }
}
