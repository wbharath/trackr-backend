package com.example.jobster_backend.service.impl;

import com.example.jobster_backend.dto.JwtAuthResponse;
import com.example.jobster_backend.dto.LoginDto;
import com.example.jobster_backend.dto.RegisterDto;
import com.example.jobster_backend.dto.UserDto;
import com.example.jobster_backend.entity.User;
import com.example.jobster_backend.exception.BadRequestException;
import com.example.jobster_backend.exception.UnauthorizedException;
import com.example.jobster_backend.repository.UserRepository;
import com.example.jobster_backend.service.AuthService;
import com.example.jobster_backend.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public JwtAuthResponse register(RegisterDto registerDto) {
        // Check if user already exists
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        // Create new user
        User user = new User();
        user.setName(registerDto.getName());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));

        // Save user
        User savedUser = userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getId());

        // Convert to DTO
        UserDto userDTO = convertToDTO(savedUser);

        return new JwtAuthResponse(userDTO, token);
    }

    @Override
    public JwtAuthResponse login(LoginDto loginDto) {
        // Find user by email
        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Check password
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        // Convert to DTO
        UserDto userDTO = convertToDTO(user);

        return new JwtAuthResponse(userDTO, token);
    }

    @Override
    public UserDto updateUser(Long userId, UserDto userDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Update fields
        if (userDTO.getName() != null && !userDTO.getName().isEmpty()) {
            user.setName(userDTO.getName());
        }

        if (userDTO.getEmail() != null && !userDTO.getEmail().isEmpty()) {
            // Check if new email is already taken by another user
            if (!user.getEmail().equals(userDTO.getEmail()) &&
                    userRepository.existsByEmail(userDTO.getEmail())) {
                throw new BadRequestException("Email already in use");
            }
            user.setEmail(userDTO.getEmail());
        }

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    private UserDto convertToDTO(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail());
    }
}