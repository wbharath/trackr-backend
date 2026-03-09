package com.example.jobster_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractResponseDto {
    private String position;
    private String company;
    private String jobLocation;
}
