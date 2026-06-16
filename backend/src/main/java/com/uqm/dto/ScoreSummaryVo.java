package com.uqm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ScoreSummaryVo {
    private Double average;
    private Double max;
    private Double min;
    private long totalScores;
    private List<CollegeScoreVo> byCollege;
    private List<ReviewItemVo> reviews;
}
