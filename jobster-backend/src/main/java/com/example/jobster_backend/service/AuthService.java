package com.example.jobster_backend.service;

import com.example.jobster_backend.dto.JwtAuthResponse;
import com.example.jobster_backend.dto.LoginDto;
import com.example.jobster_backend.dto.RegisterDto;
import com.example.jobster_backend.dto.UserDto;

public interface AuthService {
    JwtAuthResponse register(RegisterDto registerDto);
    JwtAuthResponse login(LoginDto loginDto);
    UserDto updateUser(Long userId, UserDto userDto);

}
