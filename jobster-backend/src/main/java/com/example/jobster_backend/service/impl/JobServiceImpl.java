package com.example.jobster_backend.service.impl;

import com.example.jobster_backend.dto.JobDto;
import com.example.jobster_backend.dto.MonthlyApplicationDto;
import com.example.jobster_backend.dto.StatsResponseDto;
import com.example.jobster_backend.entity.Job;
import com.example.jobster_backend.entity.JobsResponseDto;
import com.example.jobster_backend.entity.User;
import com.example.jobster_backend.exception.BadRequestException;
import com.example.jobster_backend.repository.JobRepository;
import com.example.jobster_backend.repository.UserRepository;
import com.example.jobster_backend.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    @Override
    public JobDto create(JobDto jobDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()->new BadRequestException("user not found"));

        Job job = new Job();
        job.setPosition(jobDto.getPosition());
        job.setCompany(jobDto.getCompany());
        job.setJobLocation(jobDto.getJobLocation());
        job.setJobType(jobDto.getJobType() != null ? jobDto.getJobType() : "full-time");
        job.setStatus(jobDto.getStatus() != null ? Job.JobStatus.valueOf(jobDto.getStatus().toUpperCase()) : Job.JobStatus.APPLIED);
        job.setUser(user);

        Job saved = jobRepository.save(job);
        return mapToDto(saved);
    }

    @Override
    public JobsResponseDto getAllJobs(Long userId, String search, String status,
                                      String jobType, String sort, int page) {
        int pageSize = 10;
        int offset = (page - 1) * pageSize;

        // Getting all jobs for this user
        List<Job> allJobs = jobRepository.findAllByUserId(userId);

        // Filtering by search
        if (search != null && !search.isEmpty()) {
            String lower = search.toLowerCase();
            allJobs = allJobs.stream()
                    .filter(j -> j.getPosition().toLowerCase().contains(lower)
                            || j.getCompany().toLowerCase().contains(lower))
                    .collect(Collectors.toList());
        }

        // Filtering by status
        if (status != null && !status.equals("all")) {
            allJobs = allJobs.stream()
                    .filter(j -> j.getStatus().name().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        // Filtering by jobType
        if (jobType != null && !jobType.equals("all")) {
            allJobs = allJobs.stream()
                    .filter(j -> j.getJobType().equalsIgnoreCase(jobType))
                    .collect(Collectors.toList());
        }

        // Sortingg
        Comparator<Job> comparator;
        switch (sort) {
            case "oldest":
                comparator = Comparator.comparing(Job::getId);
                break;
            case "a-z":
                comparator = Comparator.comparing(j -> j.getPosition().toLowerCase());
                break;
            case "z-a":
                comparator = Comparator.comparing((Job j) -> j.getPosition().toLowerCase()).reversed();
                break;
            default: // latest one
                comparator = Comparator.comparing(Job::getId).reversed();
                break;
        }
        allJobs = allJobs.stream().sorted(comparator).collect(Collectors.toList());

        int totalJobs = allJobs.size();
        int numOfPages = (int) Math.ceil((double) totalJobs / pageSize);

        // Pagination
        List<Job> paginated = allJobs.stream()
                .skip(offset)
                .limit(pageSize)
                .collect(Collectors.toList());

        List<JobDto> jobDtos = paginated.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new JobsResponseDto(jobDtos, totalJobs, numOfPages);
    }

    @Override
    public JobDto updateJob(Long jobId, JobDto jobDto, Long userId) {
        Job job = jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(()->new BadRequestException("Job not found or unauthorised"));

        job.setPosition(jobDto.getPosition());
        job.setCompany(jobDto.getCompany());
        job.setJobLocation(jobDto.getJobLocation());
        job.setJobType(jobDto.getJobType());
        job.setStatus(Job.JobStatus.valueOf(jobDto.getStatus().toUpperCase()));

        return mapToDto(jobRepository.save(job));
    }

    @Override
    public void deleteJob(Long jobId, Long userId) {
        Job job = jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new BadRequestException("Job not found or unauthorized"));
        jobRepository.delete(job);
    }

    @Override
    public StatsResponseDto getStats(Long userId) {

        Map<String, Long> defaultStats = new HashMap<>();
        defaultStats.put("pending", 0L);
        defaultStats.put("interview", 0L);
        defaultStats.put("declined", 0L);

        List<Object[]> statusCounts = jobRepository.countByStatusForUser(userId);
        for (Object[] row : statusCounts) {
            String status = ((Job.JobStatus) row[0]).name();
            Long count = (Long) row[1];
            defaultStats.put(status.toLowerCase(), count);
        }

        // Monthly applications — last 6 months
        List<Object[]> monthlyCounts = jobRepository.countByMonthForUser(userId);
        List<MonthlyApplicationDto> monthlyApplications = new ArrayList<>();

        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        int count = 0;
        for (Object[] row : monthlyCounts) {
            if (count >= 6) break;
            int month = ((Number) row[0]).intValue();
            int year = ((Number) row[1]).intValue();
            long jobCount = ((Number) row[2]).longValue();
            String date = monthNames[month - 1] + " " + String.valueOf(year).substring(2);
            monthlyApplications.add(new MonthlyApplicationDto(date, jobCount));
            count++;
        }

        // Reverse so oldest month is first (chart reads left to right)
        Collections.reverse(monthlyApplications);

        return new StatsResponseDto(defaultStats, monthlyApplications);
    }

    private JobDto mapToDto(Job job) {
        JobDto dto = new JobDto();
        dto.setId(job.getId());
        dto.setPosition(job.getPosition());
        dto.setCompany(job.getCompany());
        dto.setJobLocation(job.getJobLocation());
        dto.setJobType(job.getJobType());
        dto.setStatus(job.getStatus().name());
        return dto;
    }
}
