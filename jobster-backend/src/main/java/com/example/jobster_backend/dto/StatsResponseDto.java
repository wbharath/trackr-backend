package com.example.jobster_backend.dto;


import com.example.jobster_backend.entity.Job;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponseDto {
    private Map<String, Long> defaultStats;
    private List<MonthlyApplicationDto> monthlyApplications;
}
