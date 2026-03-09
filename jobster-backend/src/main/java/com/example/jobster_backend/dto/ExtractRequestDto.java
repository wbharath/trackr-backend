package com.example.jobster_backend.dto;

import lombok.Data;

@Data
public class ExtractRequestDto {
    private String pageText;
    private String pageTitle;
}
