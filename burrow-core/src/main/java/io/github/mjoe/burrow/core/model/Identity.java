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

package io.github.mjoe.burrow.core.model;

import java.util.Optional;

/**
 * Represents an SSH identity (username, password, key file).
 */
public final class Identity implements Comparable<Identity> {
    private final EntityId id;
    private final String alias;
    private final String username;
    private final String password;
    private final String keyFilePath;
    private final String keyPassphrase;

    private Identity(Builder builder) {
        this.id = builder.id;
        this.alias = builder.alias;
        this.username = builder.username;
        this.password = builder.password;
        this.keyFilePath = builder.keyFilePath;
        this.keyPassphrase = builder.keyPassphrase;
    }

    public EntityId id() {
        return id;
    }

    public String alias() {
        return alias;
    }

    public String username() {
        return username;
    }

    public Optional<String> password() {
        return Optional.ofNullable(password);
    }

    public Optional<String> keyFilePath() {
        return Optional.ofNullable(keyFilePath);
    }

    public Optional<String> keyPassphrase() {
        return Optional.ofNullable(keyPassphrase);
    }

    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }

    public boolean hasKeyFile() {
        return keyFilePath != null && !keyFilePath.isBlank();
    }

    @Override
    public int compareTo(Identity other) {
        return alias.compareToIgnoreCase(other.alias);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Identity other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Identity[alias=%s, username=%s, hasKey=%s]".formatted(alias, username, hasKeyFile());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private EntityId id;
        private String alias;
        private String username;
        private String password;
        private String keyFilePath;
        private String keyPassphrase;

        private Builder() {}

        public Builder id(EntityId id) {
            this.id = id;
            return this;
        }

        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder keyFilePath(String keyFilePath) {
            this.keyFilePath = keyFilePath;
            return this;
        }

        public Builder keyPassphrase(String keyPassphrase) {
            this.keyPassphrase = keyPassphrase;
            return this;
        }

        public Identity build() {
            if (alias == null || alias.isBlank()) {
                throw new IllegalArgumentException("Alias cannot be null or blank");
            }
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("Username cannot be null or blank");
            }
            if (id == null) {
                id = EntityId.generate("id", alias.hashCode() & Integer.MAX_VALUE);
            }
            return new Identity(this);
        }
    }
}
