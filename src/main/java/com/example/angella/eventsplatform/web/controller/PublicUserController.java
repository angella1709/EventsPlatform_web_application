package com.example.angella.eventsplatform.web.controller;

import com.example.angella.eventsplatform.entity.User;
import com.example.angella.eventsplatform.mapper.UserMapper;
import com.example.angella.eventsplatform.service.UserService;
import com.example.angella.eventsplatform.web.dto.CreateUserRequest;
import com.example.angella.eventsplatform.web.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/user")
@RequiredArgsConstructor
public class PublicUserController {

    private final UserService userService;

    private final UserMapper userMapper;

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userMapper.toDto(userService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<UserDto> registerUser(@RequestBody CreateUserRequest request) {
        var createdUser = userService.registerUser(userMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toDto(createdUser));
    }

}