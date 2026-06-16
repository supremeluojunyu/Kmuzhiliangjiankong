package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uqm.common.BusinessException;
import com.uqm.dto.CreateGroupRequest;
import com.uqm.dto.GroupManageVo;
import com.uqm.dto.UpdateGroupRequest;
import com.uqm.entity.Permission;
import com.uqm.entity.UserGroupEntity;
import com.uqm.mapper.GroupMapper;
import com.uqm.mapper.GroupPermissionMapper;
import com.uqm.mapper.PermissionMapper;
import com.uqm.mapper.UserGroupRelationMapper;
import com.uqm.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupManageService {

    private static final List<Integer> PROTECTED_GROUP_IDS = List.of(1, 2, 3, 4, 5, 6, 7);

    private final GroupMapper groupMapper;
    private final PermissionMapper permissionMapper;
    private final GroupPermissionMapper groupPermissionMapper;
    private final UserGroupRelationMapper userGroupRelationMapper;
    private final PermissionService permissionService;
    private final OperationLogService operationLogService;

    public List<GroupManageVo> listAll(LoginUser user) {
        permissionService.requirePermission(user, "group:manage");
        return groupMapper.selectList(new LambdaQueryWrapper<UserGroupEntity>()
                        .orderByAsc(UserGroupEntity::getGroupId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    public List<Permission> listPermissions(LoginUser user) {
        permissionService.requirePermission(user, "group:manage");
        return permissionMapper.selectList(null);
    }

    @Transactional
    public GroupManageVo create(LoginUser user, CreateGroupRequest request) {
        permissionService.requirePermission(user, "group:manage");
        UserGroupEntity group = new UserGroupEntity();
        group.setGroupName(request.getGroupName());
        group.setDescription(request.getDescription());
        group.setParentGroupId(request.getParentGroupId());
        groupMapper.insert(group);

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            groupPermissionMapper.batchInsert(group.getGroupId(), request.getPermissionIds());
        }

        operationLogService.log(user, "group:create", "group", group.getGroupId(), null);
        return toVo(group);
    }

    @Transactional
    public GroupManageVo update(LoginUser user, Integer groupId, UpdateGroupRequest request) {
        permissionService.requirePermission(user, "group:manage");
        UserGroupEntity group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(404, "组不存在");
        }
        if (StringUtils.hasText(request.getGroupName())) {
            group.setGroupName(request.getGroupName());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        if (request.getParentGroupId() != null) {
            group.setParentGroupId(request.getParentGroupId());
        }
        groupMapper.updateById(group);

        if (request.getPermissionIds() != null) {
            groupPermissionMapper.deleteByGroupId(groupId);
            if (!request.getPermissionIds().isEmpty()) {
                groupPermissionMapper.batchInsert(groupId, request.getPermissionIds());
            }
        }

        operationLogService.log(user, "group:update", "group", groupId, null);
        return toVo(groupMapper.selectById(groupId));
    }

    @Transactional
    public void delete(LoginUser user, Integer groupId) {
        permissionService.requirePermission(user, "group:manage");
        if (PROTECTED_GROUP_IDS.contains(groupId)) {
            throw new BusinessException(400, "内置组不可删除");
        }
        long members = userGroupRelationMapper.countMembers(groupId);
        if (members > 0) {
            throw new BusinessException(400, "组内仍有成员，无法删除");
        }
        groupPermissionMapper.deleteByGroupId(groupId);
        groupMapper.deleteById(groupId);
        operationLogService.log(user, "group:delete", "group", groupId, null);
    }

    private GroupManageVo toVo(UserGroupEntity group) {
        List<Integer> permIds = groupPermissionMapper.listPermissionIdsByGroup(group.getGroupId());
        List<String> codes = permIds.isEmpty() ? List.of()
                : permissionMapper.selectList(
                        new LambdaQueryWrapper<Permission>().in(Permission::getPermissionId, permIds))
                .stream()
                .map(Permission::getPermissionCode)
                .collect(Collectors.toList());

        return GroupManageVo.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .description(group.getDescription())
                .parentGroupId(group.getParentGroupId())
                .createdAt(group.getCreatedAt())
                .permissionIds(permIds)
                .permissionCodes(codes)
                .memberCount(userGroupRelationMapper.countMembers(group.getGroupId()))
                .build();
    }
}
