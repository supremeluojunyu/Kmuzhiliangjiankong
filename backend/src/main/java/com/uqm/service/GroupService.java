package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uqm.entity.UserGroupEntity;
import com.uqm.mapper.GroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupMapper groupMapper;

    public List<UserGroupEntity> listAll() {
        return groupMapper.selectList(new LambdaQueryWrapper<UserGroupEntity>()
                .orderByAsc(UserGroupEntity::getGroupId));
    }
}
