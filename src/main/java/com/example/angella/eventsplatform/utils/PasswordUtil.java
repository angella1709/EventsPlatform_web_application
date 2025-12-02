package com.example.angella.eventsplatform.utils;

import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordUtil {

    public static String encodePassword(String plainPassword, PasswordEncoder passwordEncoder) {
        return passwordEncoder.encode(plainPassword);
    }
}