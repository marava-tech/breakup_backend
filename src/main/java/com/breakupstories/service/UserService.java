package com.breakupstories.service;

import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.UserRequest;
import com.breakupstories.dto.UserResponse;
import com.breakupstories.dto.UserProfileResponse;
import com.breakupstories.enums.GENDER;
import com.breakupstories.enums.Role;
import com.breakupstories.exception.ResourceAlreadyExistsException;
import com.breakupstories.exception.ResourceNotFoundException;
import com.breakupstories.model.User;
import com.breakupstories.repository.UserRepository;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.repository.LikeRepository;
import com.breakupstories.repository.CommentRepository;
import com.breakupstories.repository.CoinHistoryRepository;
import com.breakupstories.dto.CoinHistoryResponse;
import com.breakupstories.util.ApplicationContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final OTPService otpService;
    private final DefaultConfigService defaultConfigService;
    private final UploadService uploadService;
    private final StoryRepository storyRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final CoinHistoryRepository coinHistoryRepository;
    private final RewardService rewardService;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password("") // Users don't have passwords in this setup
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
    
    public boolean sendOtpForRegistration(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ResourceAlreadyExistsException("User", "email", email);
        }
        return otpService.sendOtp(email);
    }
    
    public boolean sendOtpForLogin(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new ResourceNotFoundException("User", "email", email);
        }
        return otpService.sendOtp(email);
    }
    
    public UserResponse createUserAfterOtpVerification(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("User", "email", request.getEmail());
        }
        
        // Get default profile image URL based on gender
        String defaultProfileImageUrl = getDefaultProfileImageUrl(request.getGender());
        
        // Set default role to USER if not specified
        Role role = request.getRole() != null ? request.getRole() : Role.USER;
        
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .gender(request.getGender())
                .age(request.getAge())
                .profileImageUrl(defaultProfileImageUrl)
                .preferredStoryLanguage(request.getPreferredStoryLanguage())
                .role(role)
                .deviceId(request.getDeviceId()) // Store device ID for referral tracking
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Generate referral code for the new user
        RewardService rewardService = ApplicationContextProvider.getBean(RewardService.class);
        rewardService.generateReferralCode(savedUser.getId());
        
        // Process referral if referral code is provided (device-based)
        if (request.getReferralCode() != null && !request.getReferralCode().trim().isEmpty()) {
            String deviceId = request.getDeviceId();
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                rewardService.processReferral(savedUser.getId(), request.getReferralCode().toUpperCase(), deviceId);
            } else {
                log.warn("Referral code provided but no device ID, skipping referral processing for user: {}", savedUser.getId());
            }
        }
        
        log.info("Created user with role {} and device ID: {} -> {}", 
            role, request.getDeviceId(), request.getEmail());
        
        return UserResponse.fromUser(savedUser);
    }
    
    public UserResponse updateProfileImage(String userEmail, MultipartFile imageFile) {
        log.info("Updating profile image for user: {}", userEmail);
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        // Upload the image using upload service
        var newProfileImageUrl = uploadService.uploadSingleFile(imageFile);
        if(ObjectUtils.isEmpty(newProfileImageUrl)){
            log.error("profile image is null");
            throw  new RuntimeException("Unable to upload");
        }
        // Update user's profile image URL
        user.setProfileImageUrl(newProfileImageUrl);
        User updatedUser = userRepository.save(user);
        
        log.info("Profile image updated successfully for user: {} -> {}", 
            userEmail, newProfileImageUrl);
        
        return UserResponse.fromUser(updatedUser);
    }
    
    /**
     * Map device ID to user only if the user doesn't already have a device ID
     */
    public void mapDeviceIdToUser(String userEmail, String deviceId) {
        log.info("Attempting to map device ID {} to user: {}", deviceId, userEmail);
        
        // Find the user first
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        // Check if user already has a device ID - if yes, ignore the new device ID
        if (user.getDeviceId() != null && !user.getDeviceId().trim().isEmpty()) {
            log.info("User {} already has device ID: {}, ignoring new device ID: {}", 
                    userEmail, user.getDeviceId(), deviceId);
            return;
        }
        
        // Map the device ID to the user (multiple users can share the same device ID)
        user.setDeviceId(deviceId);
        userRepository.save(user);
        log.info("Successfully mapped device ID {} to user: {}", deviceId, userEmail);
    }


    
    private String getDefaultProfileImageUrl(GENDER gender) {
        try {
            String configKey = gender == GENDER.MALE ? "profile_male_image_url" : "profile_female_image_url";
            var config = defaultConfigService.getByKey(configKey);
            log.debug("Retrieved default profile image URL for {}: {}", gender, config.getValue());
            return config.getValue();
        } catch (Exception e) {
            log.warn("Failed to get default profile image URL for gender: {}. Using fallback URL", gender, e);
            // Fallback to a generic default image URL
            return "https://via.placeholder.com/150x150?text=" + gender.name();
        }
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
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        return UserResponse.fromUser(user);
    }
    
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        
        return UserResponse.fromUser(user);
    }
    
    public UserResponse updateUser(String userId, UserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setGender(request.getGender());
        user.setAge(request.getAge());
        user.setPreferredStoryLanguage(request.getPreferredStoryLanguage());
        
        // Update role if provided
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        
        User updatedUser = userRepository.save(user);
        return UserResponse.fromUser(updatedUser);
    }
    
    public void deleteUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        
        userRepository.deleteById(userId);
    }
    
    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
    
    public User getUserEntityById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
    
    public UserResponse updatePreferredStoryLanguage(String userEmail, String preferredStoryLanguage) {
        log.info("Updating preferred story language for user: {} -> {}", userEmail, preferredStoryLanguage);
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        user.setPreferredStoryLanguage(preferredStoryLanguage);
        User updatedUser = userRepository.save(user);
        
        log.info("Preferred story language updated successfully for user: {} -> {}", 
            userEmail, preferredStoryLanguage);
        
        return UserResponse.fromUser(updatedUser);
    }
    
    public UserProfileResponse getUserProfileWithStatistics(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // Calculate statistics
        Long totalStories = storyRepository.countByUserIdAndStatus(userId, com.breakupstories.model.Story.StoryStatus.ACTIVE);
        
        // Get all story IDs for this user to calculate likes and comments
        List<String> userStoryIds = storyRepository.findByUserIdAndStatus(userId, com.breakupstories.model.Story.StoryStatus.ACTIVE)
                .stream()
                .map(story -> story.getId())
                .collect(Collectors.toList());
        
        Long totalLikes = 0L;
        Long totalComments = 0L;
        Long totalViews = 0L;
        
        if (!userStoryIds.isEmpty()) {
            // Calculate total likes for all user's stories
            totalLikes = userStoryIds.stream()
                    .mapToLong(storyId -> likeRepository.countByStoryId(storyId))
                    .sum();
            
            // Calculate total comments for all user's stories
            totalComments = userStoryIds.stream()
                    .mapToLong(storyId -> commentRepository.countByStoryIdAndActiveTrue(storyId))
                    .sum();
            
            // Calculate total views for all user's stories
            totalViews = storyRepository.findByUserIdAndStatus(userId, com.breakupstories.model.Story.StoryStatus.ACTIVE)
                    .stream()
                    .mapToLong(story -> story.getViewCount() != null ? story.getViewCount() : 0L)
                    .sum();
        }
        
        // Get referral information
        String referredByUserName = null;
        if (user.getReferredBy() != null) {
            Optional<User> referrer = userRepository.findById(user.getReferredBy());
            referredByUserName = referrer.map(User::getName).orElse(null);
        }
        
        // Get referral history (coin history related to referrals)
        List<CoinHistoryResponse> referralHistory = coinHistoryRepository.findByUserId(userId)
                .stream()
                .filter(coinHistory -> coinHistory.getReason().contains("referral"))
                .map(CoinHistoryResponse::fromCoinHistory)
                .collect(Collectors.toList());
        
        // Get current coin balance from coin history
        int currentCoins = rewardService.getValidTotalCoins(userId);
        
        log.info("User profile statistics for {}: stories={}, likes={}, views={}, comments={}, coins={}, referredBy={}", 
            userId, totalStories, totalLikes, totalViews, totalComments, currentCoins, referredByUserName);
        
        return UserProfileResponse.fromUserWithReferralInfo(user, totalStories, totalLikes, totalViews, totalComments, 
                                                          referredByUserName, referralHistory, currentCoins);
    }
    
    public UserProfileResponse getUserProfile(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        return getUserProfileWithStatistics(user.getId());
    }
} 