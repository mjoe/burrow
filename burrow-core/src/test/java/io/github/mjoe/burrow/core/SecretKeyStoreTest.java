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
import io.github.mjoe.burrow.core.security.SecretKeyStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SecretKeyStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void testLoadOrCreateCreatesNewKey() throws Exception {
        var keyFile = tempDir.resolve("secret.key");
        var store = new SecretKeyStore(keyFile);

        var key = store.loadOrCreate();

        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertTrue(Files.exists(keyFile));
    }

    @Test
    void testKeyIsPersistedAndReusable() throws Exception {
        var keyFile = tempDir.resolve("secret.key");
        var store = new SecretKeyStore(keyFile);

        var key1 = store.loadOrCreate();
        var key2 = store.loadOrCreate();

        assertArrayEquals(key1.getEncoded(), key2.getEncoded(),
                "Key should be stable across reloads");
    }

    @Test
    void testRoundTripEncryptDecryptAcrossLoads() throws Exception {
        var keyFile = tempDir.resolve("secret.key");
        var store = new SecretKeyStore(keyFile);

        var key1 = store.loadOrCreate();
        var encoder1 = new PasswordEncoder(key1);
        var encrypted = encoder1.encrypt("my-password");

        var key2 = store.loadOrCreate();
        var encoder2 = new PasswordEncoder(key2);
        var decrypted = encoder2.decrypt(encrypted);

        assertEquals("my-password", decrypted,
                "Password should survive app restart via persisted key");
    }

    @Test
    void testExists() throws Exception {
        var keyFile = tempDir.resolve("secret.key");
        var store = new SecretKeyStore(keyFile);

        assertFalse(store.exists());
        store.loadOrCreate();
        assertTrue(store.exists());
    }
}
