package com.uqm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uqm.entity.UserGroupEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMapper extends BaseMapper<UserGroupEntity> {
}
