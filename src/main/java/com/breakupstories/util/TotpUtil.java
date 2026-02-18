package com.breakupstories.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class TotpUtil {

    private static final long STEP_SIZE = 30000; // 30 seconds
    private static final int WINDOW_SIZE = 1; // Allow 1 step before and after

    public static boolean verify(String secret, String code) {
        if (secret == null || code == null || code.length() != 6) {
            return false;
        }

        try {
            long currentStep = System.currentTimeMillis() / STEP_SIZE;

            // Check current step, previous step, and next step to allow for clock drift
            for (int i = -WINDOW_SIZE; i <= WINDOW_SIZE; i++) {
                if (code.equals(generateTOTP(secret, currentStep + i))) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String generateTOTP(String secret, long timeIndex)
            throws NoSuchAlgorithmException, InvalidKeyException {
        // Decode the secret (assuming Base32)
        byte[] key = decodeBase32(secret);
        byte[] data = ByteBuffer.allocate(8).putLong(timeIndex).array();

        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0xF;
        long binary = ((hash[offset] & 0x7f) << 24) |
                ((hash[offset + 1] & 0xff) << 16) |
                ((hash[offset + 2] & 0xff) << 8) |
                (hash[offset + 3] & 0xff);

        long otp = binary % 1000000;
        return String.format("%06d", otp);
    }

    private static byte[] decodeBase32(String base32) {
        String base32Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        // Remove padding/invalid chars and spaces
        String cleanInput = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");

        int numBytes = cleanInput.length() * 5 / 8;
        byte[] bytes = new byte[numBytes];

        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;

        for (char c : cleanInput.toCharArray()) {
            buffer <<= 5;
            buffer |= base32Alphabet.indexOf(c);
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bytes[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return bytes;
    }
}
