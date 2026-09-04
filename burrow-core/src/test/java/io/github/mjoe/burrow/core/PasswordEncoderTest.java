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

package io.github.mjoe.burrow.core;

import io.github.mjoe.burrow.core.security.PasswordEncoder;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    @Test
    void testEncryptDecrypt() throws Exception {
        var key = PasswordEncoder.generateKey();
        var encoder = new PasswordEncoder(key);

        var password = "my-secret-password";
        var encrypted = encoder.encrypt(password);

        assertNotNull(encrypted);
        assertNotEquals(password, encrypted);

        var decrypted = encoder.decrypt(encrypted);
        assertEquals(password, decrypted);
    }

    @Test
    void testEncryptEmptyString() throws Exception {
        var key = PasswordEncoder.generateKey();
        var encoder = new PasswordEncoder(key);

        var encrypted = encoder.encrypt("");
        assertEquals("", encrypted);

        var decrypted = encoder.decrypt("");
        assertEquals("", decrypted);
    }

    @Test
    void testEncryptNull() throws Exception {
        var key = PasswordEncoder.generateKey();
        var encoder = new PasswordEncoder(key);

        var encrypted = encoder.encrypt(null);
        assertEquals("", encrypted);

        var decrypted = encoder.decrypt(null);
        assertEquals("", decrypted);
    }

    @Test
    void testDifferentKeysFailDecryption() throws Exception {
        var key1 = PasswordEncoder.generateKey();
        var key2 = PasswordEncoder.generateKey();

        var encoder1 = new PasswordEncoder(key1);
        var encoder2 = new PasswordEncoder(key2);

        var password = "secret";
        var encrypted = encoder1.encrypt(password);

        // Decryption with different key should fail
        assertThrows(Exception.class, () -> encoder2.decrypt(encrypted));
    }

    @Test
    void testIsEncrypted() {
        assertFalse(PasswordEncoder.isEncrypted(null));
        assertFalse(PasswordEncoder.isEncrypted(""));
        assertFalse(PasswordEncoder.isEncrypted("not-base64!!!"));
        assertFalse(PasswordEncoder.isEncrypted("dGVzdA==")); // "test" in base64 (too short)

        // A proper encrypted string should be detected
        try {
            var key = PasswordEncoder.generateKey();
            var encoder = new PasswordEncoder(key);
            var encrypted = encoder.encrypt("password");
            assertTrue(PasswordEncoder.isEncrypted(encrypted));
        } catch (Exception e) {
            fail("Should not throw", e);
        }
    }

    @Test
    void testGenerateKey() throws Exception {
        var key = PasswordEncoder.generateKey();
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertEquals(256, key.getEncoded().length * 8); // 256 bits
    }
}
