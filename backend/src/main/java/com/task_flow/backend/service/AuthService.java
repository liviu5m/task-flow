package com.task_flow.backend.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.task_flow.backend.dto.RegisterUserDto;
import com.task_flow.backend.model.User;
import com.task_flow.backend.repository.UserRepository;
import java.security.SecureRandom;
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final SecureRandom RANDOM = new SecureRandom();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public User saveUser(RegisterUserDto registerUserDto) {
        Optional<User> userOptional = userRepository.findByEmail(registerUserDto.getEmail());
        if(userOptional.isPresent()) throw new RuntimeException("User already exists");
        User user = new User();
        user.setName(registerUserDto.getName());
        user.setEmail(registerUserDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerUserDto.getPassword()));
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
        user.setVerificationCode(generateVerificationCode());
        return userRepository.save(user);
    }

    private String generateVerificationCode() {
        int code = RANDOM.nextInt(1000000);
        return String.format("%06d", code);
    }

}
