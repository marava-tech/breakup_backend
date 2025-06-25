package com.breakupstories.service;

import com.breakupstories.dto.AuditRequest;
import com.breakupstories.dto.AuditResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.model.Audit;
import com.breakupstories.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditService {
    
    private final AuditRepository auditRepository;
    
    public AuditResponse createAudit(AuditRequest request) {
        Audit audit = Audit.builder()
                .userId(request.getUserId())
                .entityType(request.getEntityType())
                .actionType(request.getActionType())
                .entityId(request.getEntityId())
                .build();
        
        Audit savedAudit = auditRepository.save(audit);
        return AuditResponse.fromAudit(savedAudit);
    }
    
    public void logAudit(String userId, Audit.EntityType entityType, Audit.ActionType actionType, String entityId) {
        Audit audit = Audit.builder()
                .userId(userId)
                .entityType(entityType)
                .actionType(actionType)
                .entityId(entityId)
                .build();
        
        auditRepository.save(audit);
    }
    
    public PagedResponse<AuditResponse> getAudits(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Audit> auditPage = auditRepository.findAll(pageable);
        
        List<AuditResponse> audits = auditPage.getContent().stream()
                .map(AuditResponse::fromAudit)
                .collect(Collectors.toList());
        
        return PagedResponse.of(audits, page, size, auditPage.getTotalElements());
    }
    
    public PagedResponse<AuditResponse> getAuditsByUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Audit> auditPage = auditRepository.findByUserId(userId, pageable);
        
        List<AuditResponse> audits = auditPage.getContent().stream()
                .map(AuditResponse::fromAudit)
                .collect(Collectors.toList());
        
        return PagedResponse.of(audits, page, size, auditPage.getTotalElements());
    }
    
    public PagedResponse<AuditResponse> getAuditsByEntityType(Audit.EntityType entityType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Audit> auditPage = auditRepository.findByEntityType(entityType, pageable);
        
        List<AuditResponse> audits = auditPage.getContent().stream()
                .map(AuditResponse::fromAudit)
                .collect(Collectors.toList());
        
        return PagedResponse.of(audits, page, size, auditPage.getTotalElements());
    }
    
    public PagedResponse<AuditResponse> getAuditsByEntityId(String entityId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Audit> auditPage = auditRepository.findByEntityId(entityId, pageable);
        
        List<AuditResponse> audits = auditPage.getContent().stream()
                .map(AuditResponse::fromAudit)
                .collect(Collectors.toList());
        
        return PagedResponse.of(audits, page, size, auditPage.getTotalElements());
    }
    
    public AuditResponse getAuditById(String auditId) {
        Audit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new RuntimeException("Audit not found with ID: " + auditId));
        
        return AuditResponse.fromAudit(audit);
    }
    
    public void deleteAudit(String auditId) {
        if (!auditRepository.existsById(auditId)) {
            throw new RuntimeException("Audit not found with ID: " + auditId);
        }
        
        auditRepository.deleteById(auditId);
    }
} 