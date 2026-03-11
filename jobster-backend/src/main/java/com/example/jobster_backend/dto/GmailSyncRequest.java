package com.example.jobster_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class GmailSyncRequest {

    @NotBlank(message = "Gmail access token is required")
    private String gmailAccessToken;

//    The gmail address returned from google's userinfo endpoint
    private String userEmail;

}
