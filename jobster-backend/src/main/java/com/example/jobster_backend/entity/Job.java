package com.example.jobster_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String position;

    @Column(nullable = false)
    private String company;

    @Column(name = "job_location")
    private String jobLocation;

    @Column(nullable = false)
    private String jobType;

    //    Claude assigned categories
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

//    Gmail message Id - preventing duplicate saves
    @Column(name = "gmail_message_id", unique = true, length = 255)
    private String gmailMessageId;

//    Email subject line
    @Column(name = "email_subject", length = 500)
    private String emailSubject;


//      First 200 chars of email body
    @Column(name = "email_preview", length = 500)
    private String emailPreview;


//    Email recieve date
    @Column(name = "email_date")
    private LocalDateTime emailDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum JobStatus {
        APPLIED, INTERVIEW, REJECTED, OFFER
    }



}
