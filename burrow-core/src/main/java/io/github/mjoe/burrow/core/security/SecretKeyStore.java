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

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

/**
 * Manages the persistence of the AES encryption key used for password storage.
 *
 * The key is stored in a file with restricted permissions (owner-only).
 * If no key file exists, a new random key is generated and persisted.
 */
public final class SecretKeyStore {
    private static final String ALGORITHM = "AES";

    private final Path keyFile;

    public SecretKeyStore(Path keyFile) {
        this.keyFile = keyFile;
    }

    /**
     * Load an existing key, or generate and persist a new one if none exists.
     */
    public SecretKey loadOrCreate() throws IOException, GeneralSecurityException {
        if (Files.exists(keyFile)) {
            return load();
        }
        var key = create();
        save(key);
        return key;
    }

    /**
     * Create a new random AES key.
     */
    public static SecretKey create() throws GeneralSecurityException {
        return PasswordEncoder.generateKey();
    }

    /**
     * Load the key from the key file.
     */
    public SecretKey load() throws IOException {
        var bytes = Files.readAllBytes(keyFile);
        return new SecretKeySpec(bytes, ALGORITHM);
    }

    /**
     * Persist the key to disk with owner-only permissions.
     */
    public void save(SecretKey key) throws IOException {
        var parent = keyFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        var bytes = key.getEncoded();
        Files.write(keyFile, bytes);

        // Restrict file permissions to owner-only (POSIX)
        try {
            Files.setPosixFilePermissions(keyFile,
                    java.util.Set.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                            java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX file system (e.g. Windows) - skip
        }
    }

    /**
     * Check whether a key file exists.
     */
    public boolean exists() {
        return Files.exists(keyFile);
    }
}
