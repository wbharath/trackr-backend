package com.example.jobster_backend.controller;

import com.example.jobster_backend.dto.GmailSyncRequest;
import com.example.jobster_backend.dto.GmailSyncResponse;
import com.example.jobster_backend.entity.User;
import com.example.jobster_backend.repository.UserRepository;
import com.example.jobster_backend.service.GmailSyncService;
import com.example.jobster_backend.utils.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gmail")
@RequiredArgsConstructor
public class GmailController {

    private final GmailSyncService gmailSyncService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/sync")
    public ResponseEntity<GmailSyncResponse> syncEmails(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody GmailSyncRequest request) {

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GmailSyncResponse response = gmailSyncService.syncEmails(request, user);
        return ResponseEntity.ok(response);
    }
}