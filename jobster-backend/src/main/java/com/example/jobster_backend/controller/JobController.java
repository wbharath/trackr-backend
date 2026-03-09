package com.example.jobster_backend.controller;

import com.example.jobster_backend.dto.JobDto;
import com.example.jobster_backend.entity.JobsResponseDto;
import com.example.jobster_backend.service.JobService;
import com.example.jobster_backend.utils.JwtUtil;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JwtUtil jwtUtil;


    private Long getUserIdFromToken(String authHeader){
        String token = authHeader.substring(7);
        return jwtUtil.extractUserId(token);
    }

    @PostMapping
    public ResponseEntity<JobDto> createJob(
            @RequestHeader("Authorization")  String authHeader,
            @Valid @RequestBody JobDto jobDto){
        Long userId = getUserIdFromToken(authHeader);
        JobDto created = jobService.create(jobDto, userId);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<JobsResponseDto> getAllJobs(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "all") String jobType,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page) {

        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(jobService.getAllJobs(userId, search, status, jobType, sort, page));
    }
    @PatchMapping("/{id}")
    public ResponseEntity<JobDto> updateJob(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody JobDto jobDto) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(jobService.updateJob(id, jobDto, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteJob(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        Long userId = getUserIdFromToken(authHeader);
        jobService.deleteJob(id, userId);
        return ResponseEntity.ok("Job deleted successfully");
    }




}
