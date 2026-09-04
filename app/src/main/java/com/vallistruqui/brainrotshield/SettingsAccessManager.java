package com.vallistruqui.brainrotshield;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class SettingsAccessManager {
    private static final String PREFERENCES_NAME = "brainrot_shield_settings_access";
    private static final String KEY_PIN_SALT = "pin_salt";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKED_UNTIL = "locked_until";

    private static final int MIN_PIN_LENGTH = 6;
    private static final int MAX_PIN_LENGTH = 12;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final long INITIAL_LOCKOUT_MS = 30_000L;
    private static final long MAX_LOCKOUT_MS = 30L * 60L * 1_000L;

    enum VerificationStatus {
        SUCCESS,
        INVALID,
        LOCKED,
        ERROR
    }

    static final class VerificationResult {
        private final VerificationStatus status;
        private final long retryAfterMillis;

        private VerificationResult(VerificationStatus status, long retryAfterMillis) {
            this.status = status;
            this.retryAfterMillis = retryAfterMillis;
        }

        VerificationStatus status() {
            return status;
        }

        long retryAfterMillis() {
            return retryAfterMillis;
        }
    }

    private final SharedPreferences preferences;
    private final SecureRandom secureRandom = new SecureRandom();

    SettingsAccessManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    boolean hasPin() {
        return preferences.contains(KEY_PIN_SALT) && preferences.contains(KEY_PIN_HASH);
    }

    boolean setPin(String pin) {
        if (!isValidPin(pin)) {
            return false;
        }

        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        try {
            byte[] hash = derivePinHash(pin, salt);
            return preferences.edit()
                    .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                    .remove(KEY_FAILED_ATTEMPTS)
                    .remove(KEY_LOCKED_UNTIL)
                    .commit();
        } catch (GeneralSecurityException exception) {
            return false;
        }
    }

    VerificationResult verify(String pin, long nowMillis) {
        if (!hasPin()) {
            return new VerificationResult(VerificationStatus.ERROR, 0L);
        }

        long remainingLockout = getRemainingLockoutMillis(nowMillis);
        if (remainingLockout > 0L) {
            return new VerificationResult(VerificationStatus.LOCKED, remainingLockout);
        }

        try {
            byte[] salt = Base64.decode(
                    preferences.getString(KEY_PIN_SALT, ""), Base64.NO_WRAP);
            byte[] expectedHash = Base64.decode(
                    preferences.getString(KEY_PIN_HASH, ""), Base64.NO_WRAP);
            if (salt.length != SALT_BYTES || expectedHash.length != HASH_BITS / 8) {
                return new VerificationResult(VerificationStatus.ERROR, 0L);
            }
            byte[] suppliedHash = derivePinHash(pin == null ? "" : pin, salt);
            if (MessageDigest.isEqual(expectedHash, suppliedHash)) {
                preferences.edit()
                        .remove(KEY_FAILED_ATTEMPTS)
                        .remove(KEY_LOCKED_UNTIL)
                        .apply();
                return new VerificationResult(VerificationStatus.SUCCESS, 0L);
            }

            int failedAttempts = preferences.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
            long lockoutDuration = lockoutDurationForFailures(failedAttempts);
            SharedPreferences.Editor editor = preferences.edit()
                    .putInt(KEY_FAILED_ATTEMPTS, failedAttempts);
            if (lockoutDuration > 0L) {
                editor.putLong(KEY_LOCKED_UNTIL, nowMillis + lockoutDuration);
            }
            editor.apply();
            if (lockoutDuration > 0L) {
                return new VerificationResult(VerificationStatus.LOCKED, lockoutDuration);
            }
            return new VerificationResult(VerificationStatus.INVALID, 0L);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return new VerificationResult(VerificationStatus.ERROR, 0L);
        }
    }

    long getRemainingLockoutMillis(long nowMillis) {
        return Math.max(0L, preferences.getLong(KEY_LOCKED_UNTIL, 0L) - nowMillis);
    }

    boolean clearPin() {
        return preferences.edit().clear().commit();
    }

    static boolean isValidPin(String pin) {
        if (pin == null || pin.length() < MIN_PIN_LENGTH || pin.length() > MAX_PIN_LENGTH) {
            return false;
        }
        for (int index = 0; index < pin.length(); index++) {
            if (!Character.isDigit(pin.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    static byte[] derivePinHash(String pin, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec specification = new PBEKeySpec(
                pin.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(specification)
                    .getEncoded();
        } finally {
            specification.clearPassword();
        }
    }

    static long lockoutDurationForFailures(int failedAttempts) {
        if (failedAttempts < 5) {
            return 0L;
        }
        int level = Math.min(10, Math.max(0, (failedAttempts - 5) / 5));
        long duration = INITIAL_LOCKOUT_MS << level;
        return Math.min(MAX_LOCKOUT_MS, duration);
    }
}
