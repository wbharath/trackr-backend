package com.example.jobster_backend.service;

import com.example.jobster_backend.dto.JobDto;
import com.example.jobster_backend.entity.JobsResponseDto;

import java.util.List;

public interface JobService {
    JobDto create(JobDto jobDto, Long userId);
    JobsResponseDto getAllJobs(Long userId, String search, String status,
                               String jobType, String sort, int page);
    JobDto updateJob(Long jobId, JobDto jobDto, Long userId);
    void deleteJob(Long jobId, Long userId);
}
