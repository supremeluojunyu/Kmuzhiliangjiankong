package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uqm.common.BusinessException;
import com.uqm.common.PageResult;
import com.uqm.dto.CreateUserRequest;
import com.uqm.dto.GroupBriefVo;
import com.uqm.dto.GroupDto;
import com.uqm.dto.UpdateUserRequest;
import com.uqm.dto.UserManageVo;
import com.uqm.entity.College;
import com.uqm.entity.User;
import com.uqm.entity.UserGroupEntity;
import com.uqm.mapper.CollegeMapper;
import com.uqm.mapper.GroupMapper;
import com.uqm.mapper.UserGroupRelationMapper;
import com.uqm.mapper.UserMapper;
import com.uqm.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManageService {

    private final UserMapper userMapper;
    private final CollegeMapper collegeMapper;
    private final GroupMapper groupMapper;
    private final UserGroupRelationMapper userGroupRelationMapper;
    private final PermissionService permissionService;
    private final DataScopeService dataScopeService;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    public PageResult<UserManageVo> list(LoginUser loginUser, long page, long pageSize, String keyword) {
        permissionService.requirePermission(loginUser, "user:manage");
        Integer collegeScope = dataScopeService.mutationCollegeFilter(loginUser);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt);
        if (collegeScope != null) {
            wrapper.eq(User::getCollegeId, collegeScope);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getName, keyword).or().like(User::getAccount, keyword));
        }
        Page<User> result = userMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new PageResult<>(
                result.getRecords().stream().map(this::toVo).toList(),
                result.getTotal(),
                page,
                pageSize
        );
    }

    public UserManageVo getById(LoginUser loginUser, Integer userId) {
        permissionService.requirePermission(loginUser, "user:manage");
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        dataScopeService.validateTargetUser(loginUser, user.getCollegeId());
        return toVo(user);
    }

    @Transactional
    public UserManageVo create(LoginUser loginUser, CreateUserRequest request) {
        permissionService.requirePermission(loginUser, "user:manage");
        dataScopeService.validateTargetCollege(loginUser, request.getCollegeId());
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getAccount, request.getAccount()));
        if (exists > 0) {
            throw new BusinessException(400, "账号已存在");
        }

        User user = new User();
        user.setName(request.getName());
        user.setAccount(request.getAccount());
        user.setCollegeId(request.getCollegeId());
        user.setPassword(passwordEncoder.encode(
                StringUtils.hasText(request.getPassword()) ? request.getPassword() : "admin123"));
        user.setStatus(1);
        user.setEmail(request.getEmail());
        user.setWechatUserId(request.getWechatUserId());
        userMapper.insert(user);

        Integer defaultGroupId = request.getDefaultGroupId() != null
                ? request.getDefaultGroupId()
                : request.getGroupIds().get(0);
        userGroupRelationMapper.batchInsert(user.getUserId(), request.getGroupIds(), defaultGroupId);

        operationLogService.log(loginUser, "user:create", "user", user.getUserId(),
                java.util.Map.of("account", user.getAccount()));
        return toVo(user);
    }

    @Transactional
    public UserManageVo update(LoginUser loginUser, Integer userId, UpdateUserRequest request) {
        permissionService.requirePermission(loginUser, "user:manage");
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        dataScopeService.validateTargetUser(loginUser, user.getCollegeId());
        if (request.getCollegeId() != null) {
            dataScopeService.validateTargetCollege(loginUser, request.getCollegeId());
        }
        if (StringUtils.hasText(request.getName())) {
            user.setName(request.getName());
        }
        if (request.getCollegeId() != null) {
            user.setCollegeId(request.getCollegeId());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getEmail() != null) {
            user.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail() : null);
        }
        if (request.getWechatUserId() != null) {
            user.setWechatUserId(StringUtils.hasText(request.getWechatUserId()) ? request.getWechatUserId() : null);
        }
        userMapper.updateById(user);

        if (request.getGroupIds() != null && !request.getGroupIds().isEmpty()) {
            userGroupRelationMapper.deleteByUserId(userId);
            Integer defaultGroupId = request.getDefaultGroupId() != null
                    ? request.getDefaultGroupId()
                    : request.getGroupIds().get(0);
            userGroupRelationMapper.batchInsert(userId, request.getGroupIds(), defaultGroupId);
        }

        operationLogService.log(loginUser, "user:update", "user", userId, null);
        return toVo(userMapper.selectById(userId));
    }

    private UserManageVo toVo(User user) {
        College college = user.getCollegeId() != null ? collegeMapper.selectById(user.getCollegeId()) : null;
        List<GroupDto> groups = userGroupRelationMapper.listUserGroupsWithPending(user.getUserId());
        Integer defaultGroupId = groups.stream()
                .filter(g -> g.getIsDefault() != null && g.getIsDefault() == 1)
                .map(GroupDto::getGroupId)
                .findFirst()
                .orElse(groups.isEmpty() ? null : groups.get(0).getGroupId());

        List<GroupBriefVo> briefGroups = groups.stream()
                .map(g -> GroupBriefVo.builder().groupId(g.getGroupId()).groupName(g.getGroupName()).build())
                .toList();

        return UserManageVo.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .account(user.getAccount())
                .collegeId(user.getCollegeId())
                .collegeName(college != null ? college.getCollegeName() : null)
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .groups(briefGroups)
                .defaultGroupId(defaultGroupId)
                .email(user.getEmail())
                .wechatUserId(user.getWechatUserId())
                .build();
    }
}
