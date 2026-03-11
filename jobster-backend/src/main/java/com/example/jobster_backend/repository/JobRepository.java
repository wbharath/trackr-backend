package com.example.jobster_backend.repository;

import com.example.jobster_backend.entity.Job;
import com.example.jobster_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job,Long> {
    List<Job> findAllByUserId(long userId);
    Optional<Job> findByIdAndUserId(Long id,long userId);

    boolean existsByGmailMessageId(String gmailMessageId);

    long countByUserAndStatus(User user, Job.JobStatus status);

    // Count jobs by status for a user
    @Query("SELECT j.status, COUNT(j) FROM Job j WHERE j.user.id = :userId GROUP BY j.status")
    List<Object[]> countByStatusForUser(@Param("userId") Long userId);

    // Count jobs by month/year for a user
    @Query("SELECT MONTH(j.createdAt), YEAR(j.createdAt), COUNT(j) FROM Job j WHERE j.user.id = :userId GROUP BY YEAR(j.createdAt), MONTH(j.createdAt) ORDER BY YEAR(j.createdAt) DESC, MONTH(j.createdAt) DESC")
    List<Object[]> countByMonthForUser(@Param("userId") Long userId);


}
