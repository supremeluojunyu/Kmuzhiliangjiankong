package com.uqm.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uqm.common.BusinessException;
import com.uqm.entity.User;
import com.uqm.entity.UserSessionPref;
import com.uqm.mapper.PermissionMapper;
import com.uqm.mapper.UserMapper;
import com.uqm.mapper.UserSessionPrefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserSessionPrefMapper sessionPrefMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String account) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getAccount, account));
        if (user == null || user.getStatus() != 1) {
            throw new UsernameNotFoundException("用户不存在或已禁用");
        }
        return buildLoginUser(user, null);
    }

    public LoginUser loadUserById(Integer userId, Integer groupIdOverride) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException(401, "用户不存在或已禁用");
        }
        return buildLoginUser(user, groupIdOverride);
    }

    private LoginUser buildLoginUser(User user, Integer groupIdOverride) {
        Integer groupId = groupIdOverride;
        if (groupId == null) {
            UserSessionPref pref = sessionPrefMapper.selectById(user.getUserId());
            groupId = pref != null ? pref.getCurrentGroupId() : null;
        }
        List<String> permissions = groupId != null
                ? permissionMapper.listCodesByGroupId(groupId)
                : List.of();
        return new LoginUser(
                user.getUserId(),
                user.getAccount(),
                user.getPassword(),
                user.getName(),
                user.getCollegeId(),
                groupId,
                permissions
        );
    }
}
