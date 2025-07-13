package com.breakupstories.repository;

import com.breakupstories.model.User;
import com.breakupstories.enums.Role;
import com.breakupstories.enums.GENDER;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);

    /**
     * Find user by referral code
     */
    Optional<User> findByReferralCode(String referralCode);
    
    /**
     * Check if referral code exists
     */
    boolean existsByReferralCode(String referralCode);
    
    /**
     * Find users referred by a specific user
     */
    List<User> findByReferredBy(String referredBy);
    
    // Count methods for admin statistics
    long countByRole(Role role);
    
    long countByGender(GENDER gender);
    
    long countByCreatedAtAfter(LocalDateTime date);
    
    // Filter methods for admin user management
    Page<User> findByRole(Role role, Pageable pageable);
    
    Page<User> findByGender(GENDER gender, Pageable pageable);
    
    Page<User> findByCoinBalanceBetween(int minCoins, int maxCoins, Pageable pageable);
    
    Page<User> findByCoinBalanceGreaterThanEqual(int minCoins, Pageable pageable);
    
    Page<User> findByCoinBalanceLessThanEqual(int maxCoins, Pageable pageable);
    
    @Query("{'name': {$regex: ?0, $options: 'i'}}")
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    @Query("{'email': {$regex: ?0, $options: 'i'}}")
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    
    @Query("{'name': {$regex: ?0, $options: 'i'}, 'role': ?1}")
    Page<User> findByNameContainingIgnoreCaseAndRole(String name, Role role, Pageable pageable);
    
    @Query("{'email': {$regex: ?0, $options: 'i'}, 'role': ?1}")
    Page<User> findByEmailContainingIgnoreCaseAndRole(String email, Role role, Pageable pageable);
    
    @Query("{'name': {$regex: ?0, $options: 'i'}, 'coinBalance': {$gte: ?1, $lte: ?2}}")
    Page<User> findByNameContainingIgnoreCaseAndCoinBalanceBetween(String name, int minCoins, int maxCoins, Pageable pageable);
    
    @Query("{'email': {$regex: ?0, $options: 'i'}, 'coinBalance': {$gte: ?1, $lte: ?2}}")
    Page<User> findByEmailContainingIgnoreCaseAndCoinBalanceBetween(String email, int minCoins, int maxCoins, Pageable pageable);
    
    @Query("{'role': ?0, 'coinBalance': {$gte: ?1, $lte: ?2}}")
    Page<User> findByRoleAndCoinBalanceBetween(Role role, int minCoins, int maxCoins, Pageable pageable);
    
    @Query("{'name': {$regex: ?0, $options: 'i'}, 'role': ?1, 'coinBalance': {$gte: ?2, $lte: ?3}}")
    Page<User> findByNameContainingIgnoreCaseAndRoleAndCoinBalanceBetween(String name, Role role, int minCoins, int maxCoins, Pageable pageable);
    
    @Query("{'email': {$regex: ?0, $options: 'i'}, 'role': ?1, 'coinBalance': {$gte: ?2, $lte: ?3}}")
    Page<User> findByEmailContainingIgnoreCaseAndRoleAndCoinBalanceBetween(String email, Role role, int minCoins, int maxCoins, Pageable pageable);
    
    // Combined filtering methods for admin
    Page<User> findByNameContainingIgnoreCaseAndGender(String name, GENDER gender, Pageable pageable);
    
    Page<User> findByEmailContainingIgnoreCaseAndGender(String email, GENDER gender, Pageable pageable);
    
    Page<User> findByRoleAndGender(Role role, GENDER gender, Pageable pageable);
    
    Page<User> findByGenderAndCoinBalanceBetween(GENDER gender, int minCoins, int maxCoins, Pageable pageable);
    
    // UserId filtering methods
    Page<User> findById(String userId, Pageable pageable);
    
    Page<User> findByNameContainingIgnoreCaseAndId(String name, String userId, Pageable pageable);
    
    Page<User> findByEmailContainingIgnoreCaseAndId(String email, String userId, Pageable pageable);
    
    Page<User> findByRoleAndId(Role role, String userId, Pageable pageable);
    
    Page<User> findByGenderAndId(GENDER gender, String userId, Pageable pageable);
    
    Page<User> findByIdAndCoinBalanceBetween(String userId, int minCoins, int maxCoins, Pageable pageable);
    
    // Additional userId combinations
    @Query("{'name': {$regex: ?0, $options: 'i'}, '_id': ?1, 'coinBalance': {$gte: ?2, $lte: ?3}}")
    Page<User> findByNameContainingIgnoreCaseAndIdAndCoinBalanceBetween(String name, String userId, int minCoins, int maxCoins, Pageable pageable);
    
    @Query("{'email': {$regex: ?0, $options: 'i'}, '_id': ?1, 'coinBalance': {$gte: ?2, $lte: ?3}}")
    Page<User> findByEmailContainingIgnoreCaseAndIdAndCoinBalanceBetween(String email, String userId, int minCoins, int maxCoins, Pageable pageable);
    
    // Three-way combinations
    Page<User> findByNameContainingIgnoreCaseAndRoleAndGender(String name, Role role, GENDER gender, Pageable pageable);
    
    @Query("{'name': {$regex: ?0, $options: 'i'}, 'gender': ?1, 'coinBalance': {$gte: ?2, $lte: ?3}}")
    Page<User> findByNameContainingIgnoreCaseAndGenderAndCoinBalanceBetween(String name, GENDER gender, int minCoins, int maxCoins, Pageable pageable);
    
    Page<User> findByEmailContainingIgnoreCaseAndRoleAndGender(String email, Role role, GENDER gender, Pageable pageable);
    
    @Query("{'email': {$regex: ?0, $options: 'i'}, 'gender': ?1, 'coinBalance': {$gte: ?2, $lte: ?3}}")
    Page<User> findByEmailContainingIgnoreCaseAndGenderAndCoinBalanceBetween(String email, GENDER gender, int minCoins, int maxCoins, Pageable pageable);
    
    Page<User> findByRoleAndGenderAndCoinBalanceBetween(Role role, GENDER gender, int minCoins, int maxCoins, Pageable pageable);
    
    // Three-way combinations with userId
    Page<User> findByNameContainingIgnoreCaseAndRoleAndId(String name, Role role, String userId, Pageable pageable);
    
    Page<User> findByNameContainingIgnoreCaseAndGenderAndId(String name, GENDER gender, String userId, Pageable pageable);
    
    Page<User> findByEmailContainingIgnoreCaseAndRoleAndId(String email, Role role, String userId, Pageable pageable);
    
    Page<User> findByEmailContainingIgnoreCaseAndGenderAndId(String email, GENDER gender, String userId, Pageable pageable);
    
    Page<User> findByRoleAndGenderAndId(Role role, GENDER gender, String userId, Pageable pageable);
    
    Page<User> findByRoleAndIdAndCoinBalanceBetween(Role role, String userId, int minCoins, int maxCoins, Pageable pageable);
    
    Page<User> findByGenderAndIdAndCoinBalanceBetween(GENDER gender, String userId, int minCoins, int maxCoins, Pageable pageable);
    
    // Four-way combinations
    Page<User> findByNameContainingIgnoreCaseAndRoleAndGenderAndCoinBalanceBetween(String name, Role role, GENDER gender, int minCoins, int maxCoins, Pageable pageable);
    
    Page<User> findByEmailContainingIgnoreCaseAndRoleAndGenderAndCoinBalanceBetween(String email, Role role, GENDER gender, int minCoins, int maxCoins, Pageable pageable);
    
    // Four-way combinations with userId
    Page<User> findByNameContainingIgnoreCaseAndRoleAndGenderAndId(String name, Role role, GENDER gender, String userId, Pageable pageable);
    
    Page<User> findByEmailContainingIgnoreCaseAndRoleAndGenderAndId(String email, Role role, GENDER gender, String userId, Pageable pageable);
    
    Page<User> findByNameContainingIgnoreCaseAndRoleAndIdAndCoinBalanceBetween(String name, Role role, String userId, int minCoins, int maxCoins, Pageable pageable);
    
    Page<User> findByEmailContainingIgnoreCaseAndRoleAndIdAndCoinBalanceBetween(String email, Role role, String userId, int minCoins, int maxCoins, Pageable pageable);
    
    Page<User> findByNameContainingIgnoreCaseAndGenderAndIdAndCoinBalanceBetween(String name, GENDER gender, String userId, int minCoins, int maxCoins, Pageable pageable);
    
    Page<User> findByEmailContainingIgnoreCaseAndGenderAndIdAndCoinBalanceBetween(String email, GENDER gender, String userId, int minCoins, int maxCoins, Pageable pageable);
    
    Page<User> findByRoleAndGenderAndIdAndCoinBalanceBetween(Role role, GENDER gender, String userId, int minCoins, int maxCoins, Pageable pageable);
    
    // Five-way combinations with userId
    Page<User> findByNameContainingIgnoreCaseAndRoleAndGenderAndIdAndCoinBalanceBetween(String name, Role role, GENDER gender, String userId, int minCoins, int maxCoins, Pageable pageable);
    
    Page<User> findByEmailContainingIgnoreCaseAndRoleAndGenderAndIdAndCoinBalanceBetween(String email, Role role, GENDER gender, String userId, int minCoins, int maxCoins, Pageable pageable);
    
    // Date range methods for dashboard statistics
    long countByCreatedAtBetween(LocalDateTime fromDate, LocalDateTime toDate);
    
    long countByRoleAndCreatedAtBetween(Role role, LocalDateTime fromDate, LocalDateTime toDate);
    
    long countByGenderAndCreatedAtBetween(GENDER gender, LocalDateTime fromDate, LocalDateTime toDate);
    
    List<User> findByCreatedAtBetween(LocalDateTime fromDate, LocalDateTime toDate);
} 