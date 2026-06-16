package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uqm.entity.College;
import com.uqm.mapper.CollegeMapper;
import com.uqm.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollegeService {

    private final CollegeMapper collegeMapper;
    private final DataScopeService dataScopeService;

    public List<College> listAll() {
        return collegeMapper.selectList(new LambdaQueryWrapper<College>()
                .eq(College::getStatus, 1)
                .orderByAsc(College::getCollegeId));
    }

    public List<College> listForUser(LoginUser user) {
        Integer collegeId = dataScopeService.mutationCollegeFilter(user);
        LambdaQueryWrapper<College> wrapper = new LambdaQueryWrapper<College>()
                .eq(College::getStatus, 1)
                .orderByAsc(College::getCollegeId);
        if (collegeId != null) {
            wrapper.eq(College::getCollegeId, collegeId);
        }
        return collegeMapper.selectList(wrapper);
    }
}
