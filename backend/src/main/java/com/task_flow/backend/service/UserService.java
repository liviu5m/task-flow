package com.task_flow.backend.service;

import org.springframework.stereotype.Service;

import com.task_flow.backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

}
