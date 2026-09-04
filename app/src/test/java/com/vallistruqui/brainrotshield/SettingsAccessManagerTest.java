package com.vallistruqui.brainrotshield;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SettingsAccessManagerTest {
    @Test
    public void pinRequiresSixToTwelveDigits() {
        assertFalse(SettingsAccessManager.isValidPin(null));
        assertFalse(SettingsAccessManager.isValidPin("12345"));
        assertFalse(SettingsAccessManager.isValidPin("12345a"));
        assertTrue(SettingsAccessManager.isValidPin("123456"));
        assertTrue(SettingsAccessManager.isValidPin("123456789012"));
        assertFalse(SettingsAccessManager.isValidPin("1234567890123"));
    }

    @Test
    public void pinHashIsDeterministicForTheSameSalt() throws Exception {
        byte[] salt = new byte[16];
        salt[0] = 42;

        byte[] hash = SettingsAccessManager.derivePinHash("654321", salt);
        assertEquals(32, hash.length);
        assertArrayEquals(hash, SettingsAccessManager.derivePinHash("654321", salt));
    }

    @Test
    public void pinHashChangesWithPinOrSalt() throws Exception {
        byte[] firstSalt = new byte[16];
        byte[] secondSalt = new byte[16];
        secondSalt[0] = 1;

        String baseline = bytesToHex(SettingsAccessManager.derivePinHash("654321", firstSalt));
        assertNotEquals(baseline,
                bytesToHex(SettingsAccessManager.derivePinHash("123456", firstSalt)));
        assertNotEquals(baseline,
                bytesToHex(SettingsAccessManager.derivePinHash("654321", secondSalt)));
    }

    @Test
    public void failedAttemptsUseProgressiveLockouts() {
        assertEquals(0L, SettingsAccessManager.lockoutDurationForFailures(4));
        assertEquals(30_000L, SettingsAccessManager.lockoutDurationForFailures(5));
        assertEquals(30_000L, SettingsAccessManager.lockoutDurationForFailures(9));
        assertEquals(60_000L, SettingsAccessManager.lockoutDurationForFailures(10));
        assertEquals(30L * 60L * 1_000L,
                SettingsAccessManager.lockoutDurationForFailures(100));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }
}
