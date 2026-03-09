package com.example.jobster_backend.repository;

import com.example.jobster_backend.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job,Long> {
    List<Job> findAllByUserId(long userId);
    Optional<Job> findByIdAndUserId(Long id,long userId);
}
