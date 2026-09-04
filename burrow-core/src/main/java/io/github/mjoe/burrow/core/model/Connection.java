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
 * Represents an SSH connection to a remote host.
 */
public final class Connection implements Comparable<Connection> {
    private final EntityId id;
    private final String alias;
    private final String host;
    private final int port;
    private final EntityId identityId;
    private final boolean autoStart;
    private final boolean autoReconnect;

    private Connection(Builder builder) {
        this.id = builder.id;
        this.alias = builder.alias;
        this.host = builder.host;
        this.port = builder.port;
        this.identityId = builder.identityId;
        this.autoStart = builder.autoStart;
        this.autoReconnect = builder.autoReconnect;
    }

    public EntityId id() {
        return id;
    }

    public String alias() {
        return alias;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public EntityId identityId() {
        return identityId;
    }

    public boolean autoStart() {
        return autoStart;
    }

    public boolean autoReconnect() {
        return autoReconnect;
    }

    @Override
    public int compareTo(Connection other) {
        return alias.compareToIgnoreCase(other.alias);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Connection other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Connection[alias=%s, host=%s:%d, autoStart=%s]".formatted(alias, host, port, autoStart);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private EntityId id;
        private String alias;
        private String host;
        private int port = 22;
        private EntityId identityId;
        private boolean autoStart;
        private boolean autoReconnect = true;

        private Builder() {}

        public Builder id(EntityId id) {
            this.id = id;
            return this;
        }

        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder identityId(EntityId identityId) {
            this.identityId = identityId;
            return this;
        }

        public Builder autoStart(boolean autoStart) {
            this.autoStart = autoStart;
            return this;
        }

        public Builder autoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }

        public Connection build() {
            if (alias == null || alias.isBlank()) {
                throw new IllegalArgumentException("Alias cannot be null or blank");
            }
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("Host cannot be null or blank");
            }
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
            if (identityId == null) {
                throw new IllegalArgumentException("Identity ID cannot be null");
            }
            if (id == null) {
                id = EntityId.generate("conn", alias.hashCode() & Integer.MAX_VALUE);
            }
            return new Connection(this);
        }
    }
}
