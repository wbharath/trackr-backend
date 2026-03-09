package com.example.jobster_backend.entity;


import com.example.jobster_backend.dto.JobDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobsResponseDto {
    private List<JobDto> jobs;
    private int totalJobs;
    private int numOfPages;
}
