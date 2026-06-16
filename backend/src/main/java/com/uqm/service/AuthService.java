package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uqm.common.BusinessException;
import com.uqm.dto.GroupDto;
import com.uqm.dto.LoginRequest;
import com.uqm.dto.LoginResponse;
import com.uqm.dto.SwitchGroupRequest;
import com.uqm.dto.UserProfileResponse;
import com.uqm.entity.College;
import com.uqm.entity.User;
import com.uqm.entity.UserGroupEntity;
import com.uqm.entity.UserGroupRelation;
import com.uqm.entity.UserSessionPref;
import com.uqm.mapper.CollegeMapper;
import com.uqm.mapper.GroupMapper;
import com.uqm.mapper.PermissionMapper;
import com.uqm.mapper.UserGroupRelationMapper;
import com.uqm.mapper.UserMapper;
import com.uqm.mapper.UserSessionPrefMapper;
import com.uqm.security.JwtTokenProvider;
import com.uqm.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String GROUP_CACHE_PREFIX = "uqm:user:group:";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final CollegeMapper collegeMapper;
    private final GroupMapper groupMapper;
    private final UserGroupRelationMapper userGroupRelationMapper;
    private final UserSessionPrefMapper sessionPrefMapper;
    private final PermissionMapper permissionMapper;
    private final StringRedisTemplate redisTemplate;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getAccount(), request.getPassword()));
        LoginUser loginUser = (LoginUser) auth.getPrincipal();
        User user = userMapper.selectById(loginUser.getUserId());
        return completeLogin(user, "auth:login");
    }

    @Transactional
    public LoginResponse loginByExternalAccount(String account, boolean autoProvision, Integer defaultGroupId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getAccount, account));
        if (user == null) {
            if (!autoProvision) {
                throw new BusinessException(403, "用户不存在，请联系管理员开通账号或开启自动建档");
            }
            user = new User();
            user.setAccount(account);
            user.setName(account);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setStatus(1);
            userMapper.insert(user);
            int groupId = defaultGroupId != null ? defaultGroupId : 6;
            userGroupRelationMapper.batchInsert(user.getUserId(), List.of(groupId), groupId);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已禁用");
        }
        return completeLogin(user, "auth:external_login");
    }

    private LoginResponse completeLogin(User user, String logAction) {
        Integer currentGroupId = resolveCurrentGroupId(user.getUserId());
        cacheCurrentGroup(user.getUserId(), currentGroupId);

        String token = tokenProvider.createToken(user.getUserId(), user.getAccount());
        List<GroupDto> groups = userGroupRelationMapper.listUserGroupsWithPending(user.getUserId());
        List<String> permissions = permissionMapper.listCodesByGroupId(currentGroupId);
        UserGroupEntity group = groupMapper.selectById(currentGroupId);

        LoginUser logUser = new LoginUser(user.getUserId(), user.getAccount(), user.getPassword(),
                user.getName(), user.getCollegeId(), currentGroupId, permissions);
        operationLogService.log(logUser, logAction, "user", user.getUserId(), null);

        return LoginResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .name(user.getName())
                .currentGroupId(currentGroupId)
                .currentGroupName(group != null ? group.getGroupName() : null)
                .permissions(permissions)
                .groups(groups)
                .build();
    }

    @Transactional
    public UserProfileResponse switchGroup(LoginUser loginUser, SwitchGroupRequest request) {
        UserGroupRelation relation = userGroupRelationMapper.selectOne(
                new LambdaQueryWrapper<UserGroupRelation>()
                        .eq(UserGroupRelation::getUserId, loginUser.getUserId())
                        .eq(UserGroupRelation::getGroupId, request.getGroupId()));
        if (relation == null) {
            throw new BusinessException(403, "您不属于该用户组");
        }

        UserSessionPref pref = sessionPrefMapper.selectById(loginUser.getUserId());
        if (pref == null) {
            pref = new UserSessionPref();
            pref.setUserId(loginUser.getUserId());
            pref.setCurrentGroupId(request.getGroupId());
            pref.setUpdatedAt(LocalDateTime.now());
            sessionPrefMapper.insert(pref);
        } else {
            pref.setCurrentGroupId(request.getGroupId());
            pref.setUpdatedAt(LocalDateTime.now());
            sessionPrefMapper.updateById(pref);
        }

        cacheCurrentGroup(loginUser.getUserId(), request.getGroupId());
        return buildProfile(loginUser.getUserId(), request.getGroupId());
    }

    public List<GroupDto> listGroups(Integer userId) {
        return userGroupRelationMapper.listUserGroupsWithPending(userId);
    }

    public UserProfileResponse getProfile(Integer userId, Integer groupId) {
        Integer currentGroupId = groupId != null ? groupId : resolveCurrentGroupId(userId);
        return buildProfile(userId, currentGroupId);
    }

    private UserProfileResponse buildProfile(Integer userId, Integer currentGroupId) {
        User user = userMapper.selectById(userId);
        College college = user.getCollegeId() != null ? collegeMapper.selectById(user.getCollegeId()) : null;
        UserGroupEntity group = groupMapper.selectById(currentGroupId);
        List<String> permissions = permissionMapper.listCodesByGroupId(currentGroupId);
        List<GroupDto> groups = userGroupRelationMapper.listUserGroupsWithPending(userId);

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .account(user.getAccount())
                .collegeId(user.getCollegeId())
                .collegeName(college != null ? college.getCollegeName() : null)
                .currentGroupId(currentGroupId)
                .currentGroupName(group != null ? group.getGroupName() : null)
                .permissions(permissions)
                .groups(groups)
                .build();
    }

    private Integer resolveCurrentGroupId(Integer userId) {
        String cached = redisTemplate.opsForValue().get(GROUP_CACHE_PREFIX + userId);
        if (cached != null) {
            return Integer.valueOf(cached);
        }
        UserSessionPref pref = sessionPrefMapper.selectById(userId);
        if (pref != null) {
            return pref.getCurrentGroupId();
        }
        UserGroupRelation defaultGroup = userGroupRelationMapper.selectOne(
                new LambdaQueryWrapper<UserGroupRelation>()
                        .eq(UserGroupRelation::getUserId, userId)
                        .eq(UserGroupRelation::getIsDefault, 1)
                        .last("LIMIT 1"));
        if (defaultGroup != null) {
            return defaultGroup.getGroupId();
        }
        UserGroupRelation any = userGroupRelationMapper.selectOne(
                new LambdaQueryWrapper<UserGroupRelation>()
                        .eq(UserGroupRelation::getUserId, userId)
                        .last("LIMIT 1"));
        if (any == null) {
            throw new BusinessException(403, "用户未分配任何组");
        }
        return any.getGroupId();
    }

    private void cacheCurrentGroup(Integer userId, Integer groupId) {
        redisTemplate.opsForValue().set(
                GROUP_CACHE_PREFIX + userId,
                String.valueOf(groupId),
                2, TimeUnit.HOURS);
    }
}
