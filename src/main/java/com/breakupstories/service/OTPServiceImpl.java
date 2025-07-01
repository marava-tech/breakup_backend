package com.breakupstories.service;

import com.breakupstories.exception.EmailSendException;
import com.breakupstories.exception.InvalidOTPException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTPServiceImpl implements OTPService {

    private final GmailSender gmailSender;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ConcurrentHashMap<String, Long> otpExpiryMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> emailOtpMap = new ConcurrentHashMap<>();

    private String generateOtp() {
        String otp = String.format("%06d", RANDOM.nextInt(1000000));
        otpExpiryMap.put(otp, System.currentTimeMillis() + 10 * 60 * 1000); // 10 minutes expiry
        log.info("Generated OTP: {}", otp);
        return otp;
    }

    private String getUnFormattedGmailContent() {
        return """
                <html>
                            <head>
                                <style>
                                    body {
                                        font-family: Arial, sans-serif;
                                        background-color: #f9f9f9;
                                        color: #333;
                                        margin: 0;
                                        padding: 0;
                                    }
                                    .container {
                                        max-width: 600px;
                                        margin: 20px auto;
                                        background-color: #fff;
                                        border-radius: 8px;
                                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                                        overflow: hidden;
                                    }
                                    .header {
                                        background-color: #e91e63;
                                        color: #fff;
                                        padding: 10px 20px;
                                        text-align: center;
                                    }
                                    .logo {
                                        margin: 20px auto;
                                        display: block;
                                        max-width: 150px;
                                    }
                                    .content {
                                        padding: 20px;
                                        text-align: center;
                                    }
                                    .otp {
                                        font-size: 24px;
                                        font-weight: bold;
                                        color: #e91e63;
                                    }
                                    .footer {
                                        background-color: #f1f1f1;
                                        padding: 10px 20px;
                                        text-align: center;
                                        font-size: 12px;
                                        color: #666;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <h2>Welcome to Breakup Stories!</h2>
                                    </div>
                                    <div class="content">
                                        <p>Dear User,</p>
                                        <p>Your verification OTP is:</p>
                                        <p class="otp">%s</p>
                                        <p>Please use this OTP within 10 minutes to complete your action.</p>
                                        <p>If you didn't request this OTP, please ignore this email.</p>
                                    </div>
                                    <div class="footer">
                                        <p>&copy; 2024 Breakup Stories. All rights reserved.</p>
                                    </div>
                                </div>
                            </body>
                        </html>
                """;
    }

    @Override
    public boolean sendOtp(String email) {
        var otp = generateOtp();
        emailOtpMap.put(email, otp);
        try {
            var gmailContent = getUnFormattedGmailContent();
            var formattedGmailContent = String.format(gmailContent, otp);
            gmailSender.sendGmail(email, "Breakup Stories Verification OTP", formattedGmailContent);
            log.info("Successfully sent OTP to email: {}", email);
            return true;
        } catch (Exception e) {
            log.error("Exception while sending OTP {} to email {}: {}", otp, email, e.getMessage());
            // Remove the OTP from maps since sending failed
            emailOtpMap.remove(email);
            otpExpiryMap.remove(otp);
            throw new EmailSendException("Failed to send OTP to email: " + email, e);
        }
    }

    @Override
    public boolean verifyOtp(String email, String providedOtp) {
        try {
            String storedOtp = emailOtpMap.get(email);
            if (storedOtp == null) {
                throw new InvalidOTPException("No OTP found for email: " + email);
            }
            
            Long expiryTime = otpExpiryMap.get(storedOtp);
            if (expiryTime != null && System.currentTimeMillis() > expiryTime) {
                // Remove expired OTP
                emailOtpMap.remove(email);
                otpExpiryMap.remove(storedOtp);
                throw new InvalidOTPException("OTP has expired for email: " + email);
            }
            
            if (storedOtp.equals(providedOtp)) {
                // Remove the OTP after successful verification
                emailOtpMap.remove(email);
                otpExpiryMap.remove(storedOtp);
                log.info("OTP verified successfully for email: {}", email);
                return true;
            } else {
                throw new InvalidOTPException("Invalid OTP provided for email: " + email);
            }
        } catch (InvalidOTPException e) {
            throw e;
        } catch (Exception e) {
            log.error("Exception while verifying OTP for email {}: {}", email, e.getMessage());
            throw new InvalidOTPException("Error occurred while verifying OTP for email: " + email, e);
        }
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    public void clearExpiredOtps() {
        var otps = otpExpiryMap.keySet();
        log.info("{} OTPs found in OTP expiry map", otps.size());
        for (var otp : otps) {
            log.debug("Checking if current OTP {} is expired", otp);
            Long expiryTime = otpExpiryMap.get(otp);
            if (expiryTime != null && System.currentTimeMillis() > expiryTime) {
                otpExpiryMap.remove(otp);
                // Also remove from emailOtpMap
                emailOtpMap.entrySet().removeIf(entry -> entry.getValue().equals(otp));
                log.info("Expired OTP removed: {}", otp);
            }
        }
    }
} 