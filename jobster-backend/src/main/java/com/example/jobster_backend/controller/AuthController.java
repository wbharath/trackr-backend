package com.example.jobster_backend.controller;

import com.example.jobster_backend.dto.JwtAuthResponse;
import com.example.jobster_backend.dto.LoginDto;
import com.example.jobster_backend.dto.RegisterDto;
import com.example.jobster_backend.dto.UserDto;
import com.example.jobster_backend.service.AuthService;
import com.example.jobster_backend.utils.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<JwtAuthResponse> register(@Valid @RequestBody RegisterDto registerDto){
        JwtAuthResponse response = authService.register(registerDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(@Valid @RequestBody LoginDto loginDto){
        JwtAuthResponse response = authService.login(loginDto);
        return ResponseEntity.ok(response);

    }

    @PatchMapping("/updateUser")
    public ResponseEntity<JwtAuthResponse> updateUser(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UserDto userDto){

        String jwt = token.substring(7);
        Long userId = jwtUtil.extractUserId(jwt);


        UserDto updateUser = authService.updateUser(userId, userDto);

        String newToken = jwtUtil.generateToken(updateUser.getEmail(), updateUser.getId());

        JwtAuthResponse response = new JwtAuthResponse(updateUser, newToken);
        return ResponseEntity.ok(response);


    }

}
