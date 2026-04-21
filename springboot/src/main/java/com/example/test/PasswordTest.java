package com.example.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String rawPassword = "123456";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("New BCrypt password: " + encodedPassword);
        
        String existingPassword = "$2a$10$N.zmdr9k7uOsxVPXnEo.FeK63P7dZuOEuK.Qd0QzWQm8VfWKmYQKG";
        boolean matches = encoder.matches(rawPassword, existingPassword);
        System.out.println("Existing password matches: " + matches);
        
        boolean newMatches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("New password matches: " + newMatches);
    }
}