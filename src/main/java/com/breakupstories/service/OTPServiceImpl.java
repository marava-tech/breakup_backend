package com.breakupstories.service;

import com.breakupstories.exception.EmailSendException;
import com.breakupstories.exception.InvalidOTPException;
import com.breakupstories.util.TotpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

import com.breakupstories.util.TimestampUtil;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTPServiceImpl implements OTPService {

    private final GmailSender gmailSender;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.totp-secret}")
    private String adminTotpSecret;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ConcurrentHashMap<String, Long> otpExpiryMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> emailOtpMap = new ConcurrentHashMap<>();

    private String generateOtp() {
        String otp = String.format("%06d", RANDOM.nextInt(1000000));
        otpExpiryMap.put(otp, TimestampUtil.currentEpochMillis() + 10 * 60 * 1000); // 10 minutes expiry
        log.info("Generated OTP: {}", otp);
        return otp;
    }

    private String getUnFormattedGmailContent() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #ffffff; margin: 0; padding: 0; color: #1a1a1a; -webkit-font-smoothing: antialiased; }
                        .wrapper { padding: 60px 20px; text-align: center; }
                        .container { max-width: 440px; margin: 0 auto; }
                        .brand { font-size: 18px; font-weight: 700; color: #e91e63; margin-bottom: 48px; letter-spacing: -0.4px; text-transform: uppercase; }
                        .content-box { background-color: #ffffff; border: 1px solid #f0f0f0; border-radius: 16px; padding: 40px 32px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); }
                        .title { font-size: 22px; font-weight: 600; margin-bottom: 12px; color: #000000; }
                        .subtitle { font-size: 15px; color: #666666; margin-bottom: 32px; line-height: 1.5; }
                        .otp-wrapper { background-color: #fff0f5; border-radius: 12px; padding: 24px; margin-bottom: 24px; }
                        .otp { font-size: 38px; font-weight: 800; color: #e91e63; letter-spacing: 8px; margin: 0; font-family: ui-monospace, 'SFMono-Regular', 'SF Mono', Menlo, Monaco, Consolas, monospace; }
                        .expiry { font-size: 13px; color: #999999; margin-bottom: 16px; }
                        .notice { font-size: 12px; color: #c0c0c0; line-height: 1.4; }
                        .footer { margin-top: 48px; font-size: 12px; color: #bdc3c7; }
                    </style>
                </head>
                <body>
                    <div class="wrapper">
                        <div class="container">
                            <div class="brand">Breakup Stories</div>
                            <div class="content-box">
                                <h1 class="title">Verification Code</h1>
                                <p class="subtitle">Use the code below to securely sign in to your account.</p>
                                <div class="otp-wrapper">
                                    <p class="otp">%s</p>
                                </div>
                                <p class="expiry">Valid for 10 minutes</p>
                                <p class="notice">If you didn't request this code, you can safely ignore this email.</p>
                            </div>
                            <div class="footer">
                                &copy; %s Breakup Stories
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """;
    }

    @Override
    public boolean sendOtp(String email) {
        // Prepare email for comparison
        String normalizedEmail = email != null ? email.trim() : "";

        // Admin Auth Flow: Skip sending email, rely on TOTP
        if (normalizedEmail.equalsIgnoreCase(adminEmail)) {
            log.info("Admin login attempt for email: {}. Skipping email OTP, expecting TOTP.", email);
            return true;
        }

        var otp = generateOtp();
        emailOtpMap.put(email, otp);
        try {
            var gmailContent = getUnFormattedGmailContent();
            var formattedGmailContent = String.format(gmailContent, otp,
                    TimestampUtil.currentLocalDateTime().getYear());
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
        String normalizedEmail = email != null ? email.trim() : "";

        // Admin Auth Flow: Verify TOTP
        if (normalizedEmail.equalsIgnoreCase(adminEmail)) {
            log.info("Verifying TOTP for admin: {}", email);
            boolean isValid = TotpUtil.verify(adminTotpSecret, providedOtp);
            if (isValid) {
                log.info("Admin TOTP verified successfully.");
                return true;
            } else {
                log.warn("Invalid TOTP provided for admin: {}", email);
                throw new InvalidOTPException("Invalid Authenticator Code");
            }
        }

        try {
            String storedOtp = emailOtpMap.get(email);
            if (storedOtp == null) {
                throw new InvalidOTPException("No OTP found for email: " + email);
            }

            Long expiryTime = otpExpiryMap.get(storedOtp);
            if (expiryTime != null && TimestampUtil.currentEpochMillis() > expiryTime) {
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
            if (expiryTime != null && TimestampUtil.currentEpochMillis() > expiryTime) {
                otpExpiryMap.remove(otp);
                // Also remove from emailOtpMap
                emailOtpMap.entrySet().removeIf(entry -> entry.getValue().equals(otp));
                log.info("Expired OTP removed: {}", otp);
            }
        }
    }
}
