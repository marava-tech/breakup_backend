package com.breakupstories.controller;

import com.breakupstories.dto.NotificationResponse;
import com.breakupstories.service.AuditService;
import com.breakupstories.service.ClientInfoService;
import com.breakupstories.service.NotificationService;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Notification management APIs")
public class NotificationController {
    
    private final NotificationService notificationService;
    private final UserService userService;
    private final AuditService auditService;
    private final ClientInfoService clientInfoService;
    
    @GetMapping
    @Operation(summary = "Get notifications", description = "Get notifications for the authenticated user")
    public ResponseEntity<NotificationResponse> getNotifications(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        log.info("Getting notifications for user: {}", userId);
        
        // Audit notification view
        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();

        NotificationResponse response = notificationService.getNotifications(userId);
        auditService.logNotificationView(userId, clientInfo.getUserAgent(),
                clientInfo.getIpAddress(), clientInfo.getSessionId());
        log.info("Audited notification view for user: {}", userId);
        return ResponseEntity.ok(response);
    }
} 