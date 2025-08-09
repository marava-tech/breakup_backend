package com.breakupstories.exception;

public class AccountBannedException extends RuntimeException {
    
    private final String email;
    private final String deviceId;
    private final String banReason;
    
    public AccountBannedException(String email, String deviceId, String banReason) {
        super(String.format("Account with email '%s' is banned. Device: %s, Reason: %s", 
                email, deviceId, banReason));
        this.email = email;
        this.deviceId = deviceId;
        this.banReason = banReason;
    }
    
    public AccountBannedException(String email, String banReason) {
        super(String.format("Account with email '%s' is banned. Reason: %s", email, banReason));
        this.email = email;
        this.deviceId = null;
        this.banReason = banReason;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public String getBanReason() {
        return banReason;
    }
}
