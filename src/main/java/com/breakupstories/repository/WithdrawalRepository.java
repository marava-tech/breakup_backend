package com.breakupstories.repository;

import com.breakupstories.model.Withdrawal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WithdrawalRepository extends MongoRepository<Withdrawal, String> {
    
    Page<Withdrawal> findByUserId(String userId, Pageable pageable);
    
    Page<Withdrawal> findByStatus(Withdrawal.WithdrawalStatus status, Pageable pageable);
    
    Page<Withdrawal> findByUserIdAndStatus(String userId, Withdrawal.WithdrawalStatus status, Pageable pageable);
    
    List<Withdrawal> findByUserIdAndStatus(String userId, Withdrawal.WithdrawalStatus status);
    
    long countByUserId(String userId);
    
    long countByStatus(Withdrawal.WithdrawalStatus status);
    
    long countByCreatedAtAfter(LocalDateTime dateTime);
} 