package com.task_flow.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.task_flow.backend.dto.RegisterUserDto;
import com.task_flow.backend.model.User;
import com.task_flow.backend.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody RegisterUserDto registerUserDto) {
        if(!registerUserDto.getPasswordConfirmation().equals(registerUserDto.getPassword())) throw new RuntimeException("Passwords do not match");
        User user = authService.saveUser(registerUserDto);
        return ResponseEntity.ok(user);
    }
}
