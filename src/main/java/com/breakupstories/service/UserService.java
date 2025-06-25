package com.breakupstories.service;

import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.UserRequest;
import com.breakupstories.dto.UserResponse;
import com.breakupstories.model.User;
import com.breakupstories.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password("") // Users don't have passwords in this setup
                .authorities("USER")
                .build();
    }
    
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User with email " + request.getEmail() + " already exists");
        }
        
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .profileImageUrl(request.getProfileImageUrl())
                .authProvider(User.AuthProvider.GOOGLE)
                .build();
        
        User savedUser = userRepository.save(user);
        return UserResponse.fromUser(savedUser);
    }
    
    public UserResponse createUserFromOAuth(String email, String name, String profileImageUrl) {
        return userRepository.findByEmail(email)
                .map(existingUser -> {
                    existingUser.setName(name);
                    existingUser.setProfileImageUrl(profileImageUrl);
                    return UserResponse.fromUser(userRepository.save(existingUser));
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .name(name)
                            .email(email)
                            .profileImageUrl(profileImageUrl)
                            .authProvider(User.AuthProvider.GOOGLE)
                            .build();
                    return UserResponse.fromUser(userRepository.save(newUser));
                });
    }
    
    public PagedResponse<UserResponse> getUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);
        
        List<UserResponse> users = userPage.getContent().stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
        
        return PagedResponse.of(users, page, size, userPage.getTotalElements());
    }
    
    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        return UserResponse.fromUser(user);
    }
    
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        return UserResponse.fromUser(user);
    }
    
    public UserResponse updateUser(String userId, UserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setProfileImageUrl(request.getProfileImageUrl());
        
        User updatedUser = userRepository.save(user);
        return UserResponse.fromUser(updatedUser);
    }
    
    public void deleteUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with ID: " + userId);
        }
        
        userRepository.deleteById(userId);
    }
    
    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }
} 