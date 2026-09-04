/*
 * Copyright 2026 Burrow Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.mjoe.burrow.core.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Secure password storage using AES-GCM encryption.
 * Unlike the original jentunnel, this uses a randomly generated key
 * stored in a separate key file, not hardcoded.
 */
public final class PasswordEncoder {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_LENGTH = 256;

    private final SecretKey secretKey;

    public PasswordEncoder(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Generate a new random key for password encryption.
     */
    public static SecretKey generateKey() throws Exception {
        var keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(KEY_LENGTH, new SecureRandom());
        return keyGen.generateKey();
    }

    /**
     * Encrypt a password string.
     */
    public String encrypt(String password) throws Exception {
        if (password == null || password.isEmpty()) {
            return "";
        }

        var cipher = Cipher.getInstance(ALGORITHM);
        var iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        var spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

        var encrypted = cipher.doFinal(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // Combine IV and encrypted data
        var combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypt an encrypted password string.
     */
    public String decrypt(String encryptedPassword) throws Exception {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return "";
        }

        var combined = Base64.getDecoder().decode(encryptedPassword);

        // Extract IV and encrypted data
        var iv = new byte[GCM_IV_LENGTH];
        var encrypted = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

        var cipher = Cipher.getInstance(ALGORITHM);
        var spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        var decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Check if a string appears to be encrypted (Base64 with correct length).
     */
    public static boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            var decoded = Base64.getDecoder().decode(value);
            return decoded.length > GCM_IV_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
