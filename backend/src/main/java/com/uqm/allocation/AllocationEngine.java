package com.uqm.allocation;

import com.uqm.dto.UserPoolItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class AllocationEngine {

    /**
     * 手动 / 按学院：指定学院内目标组每个用户各生成一个实例
     */
    public List<UserPoolItem> allocateManual(List<UserPoolItem> userPool) {
        return new ArrayList<>(userPool);
    }

    /**
     * 随机分配：按学院人数比例分配 totalInstances，再随机抽取用户
     */
    public List<UserPoolItem> allocateRandom(List<UserPoolItem> userPool,
                                              List<Integer> collegeIds,
                                              Integer totalInstances) {
        if (userPool.isEmpty()) {
            return List.of();
        }
        if (totalInstances == null || totalInstances <= 0) {
            totalInstances = userPool.size();
        }

        Map<Integer, List<UserPoolItem>> usersByCollege = userPool.stream()
                .collect(Collectors.groupingBy(UserPoolItem::getCollegeId));

        Map<Integer, Integer> collegeUserCounts = new HashMap<>();
        for (Map.Entry<Integer, List<UserPoolItem>> e : usersByCollege.entrySet()) {
            collegeUserCounts.put(e.getKey(), e.getValue().size());
        }

        int totalUsers = collegeUserCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (totalUsers == 0) {
            return List.of();
        }

        Map<Integer, Integer> allocation = new HashMap<>();
        int remaining = totalInstances;
        for (Map.Entry<Integer, Integer> entry : collegeUserCounts.entrySet()) {
            int college = entry.getKey();
            int count = entry.getValue();
            int target = Math.round(totalInstances * count / (float) totalUsers);
            target = Math.min(target, count);
            allocation.put(college, target);
            remaining -= target;
        }

        Random random = new Random();
        List<Integer> eligibleColleges = collegeIds != null && !collegeIds.isEmpty()
                ? collegeIds
                : new ArrayList<>(usersByCollege.keySet());

        while (remaining > 0) {
            List<Integer> eligible = eligibleColleges.stream()
                    .filter(c -> allocation.getOrDefault(c, 0) < usersByCollege.getOrDefault(c, List.of()).size())
                    .toList();
            if (eligible.isEmpty()) {
                break;
            }
            int chosen = eligible.get(random.nextInt(eligible.size()));
            allocation.put(chosen, allocation.getOrDefault(chosen, 0) + 1);
            remaining--;
        }

        List<UserPoolItem> instances = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : allocation.entrySet()) {
            List<UserPoolItem> users = usersByCollege.get(entry.getKey());
            if (users == null || users.isEmpty()) {
                continue;
            }
            List<UserPoolItem> shuffled = new ArrayList<>(users);
            Collections.shuffle(shuffled, random);
            int num = Math.min(entry.getValue(), shuffled.size());
            instances.addAll(shuffled.subList(0, num));
        }
        return instances;
    }

    /**
     * 按总量分配：与随机类似，但 totalInstances 必填
     */
    public List<UserPoolItem> allocateByTotal(List<UserPoolItem> userPool,
                                               List<Integer> collegeIds,
                                               int totalInstances) {
        if (totalInstances <= 0) {
            throw new IllegalArgumentException("总任务量必须大于0");
        }
        return allocateRandom(userPool, collegeIds, totalInstances);
    }
}
