package com.uqm.util;

import com.uqm.common.BusinessException;
import com.uqm.dto.FlowNode;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

public final class ScoreConfigUtil {

    public static final String MODE_NUMERIC = "numeric";
    public static final String MODE_GRADE = "grade";
    public static final List<String> DEFAULT_GRADES = List.of("优", "良", "中", "差");

    private ScoreConfigUtil() {
    }

    public static String scoreMode(FlowNode node) {
        if (node == null || node.getConfig() == null) {
            return MODE_NUMERIC;
        }
        Object mode = node.getConfig().get("scoreMode");
        return MODE_GRADE.equals(String.valueOf(mode)) ? MODE_GRADE : MODE_NUMERIC;
    }

    @SuppressWarnings("unchecked")
    public static List<String> gradeOptions(FlowNode node) {
        if (node == null || node.getConfig() == null) {
            return DEFAULT_GRADES;
        }
        Object options = node.getConfig().get("gradeOptions");
        if (options instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(String::valueOf).toList();
        }
        return DEFAULT_GRADES;
    }

    public static void validateScoreSubmit(FlowNode node, Map<String, Object> data) {
        if (MODE_GRADE.equals(scoreMode(node))) {
            Object grade = data.get("grade");
            if (grade == null || !StringUtils.hasText(String.valueOf(grade))) {
                throw new BusinessException(400, "请选择等级");
            }
            if (!gradeOptions(node).contains(String.valueOf(grade))) {
                throw new BusinessException(400, "无效的等级：" + grade);
            }
        } else {
            if (!data.containsKey("score")) {
                throw new BusinessException(400, "请填写评分");
            }
            double score = toDouble(data.get("score"));
            if (score < 0 || score > 100) {
                throw new BusinessException(400, "评分须在 0–100 之间");
            }
        }
    }

    private static double toDouble(Object val) {
        if (val == null) {
            throw new BusinessException(400, "请填写评分");
        }
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        String text = String.valueOf(val).trim();
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(400, "请填写评分");
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "请填写有效评分");
        }
    }
}
