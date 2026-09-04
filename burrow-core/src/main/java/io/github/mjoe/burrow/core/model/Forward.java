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

import java.net.InetSocketAddress;

/**
 * Represents a port forwarding rule.
 */
public sealed abstract class Forward implements Comparable<Forward>
        permits Forward.Local, Forward.Remote, Forward.Dynamic {

    private final EntityId id;
    private final EntityId connectionId;
    private final String alias;

    protected Forward(EntityId id, EntityId connectionId, String alias) {
        this.id = id;
        this.connectionId = connectionId;
        this.alias = alias;
    }

    public EntityId id() {
        return id;
    }

    public EntityId connectionId() {
        return connectionId;
    }

    public String alias() {
        return alias;
    }

    public abstract ForwardType type();

    @Override
    public int compareTo(Forward other) {
        return alias.compareToIgnoreCase(other.alias);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Forward other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public enum ForwardType {
        LOCAL, REMOTE, DYNAMIC
    }

    /**
     * Local port forwarding (ssh -L).
     * Binds a local port and forwards traffic to a remote destination.
     */
    public static final class Local extends Forward {
        private final InetSocketAddress localAddress;
        private final InetSocketAddress remoteAddress;

        private Local(EntityId id, EntityId connectionId, String alias,
                      InetSocketAddress localAddress, InetSocketAddress remoteAddress) {
            super(id, connectionId, alias);
            this.localAddress = localAddress;
            this.remoteAddress = remoteAddress;
        }

        public InetSocketAddress localAddress() {
            return localAddress;
        }

        public InetSocketAddress remoteAddress() {
            return remoteAddress;
        }

        @Override
        public ForwardType type() {
            return ForwardType.LOCAL;
        }

        @Override
        public String toString() {
            return "LocalForward[%s: %s -> %s]".formatted(alias(), localAddress, remoteAddress);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private EntityId id;
            private EntityId connectionId;
            private String alias;
            private InetSocketAddress localAddress;
            private InetSocketAddress remoteAddress;

            private Builder() {}

            public Builder id(EntityId id) { this.id = id; return this; }
            public Builder connectionId(EntityId connectionId) { this.connectionId = connectionId; return this; }
            public Builder alias(String alias) { this.alias = alias; return this; }
            public Builder localAddress(InetSocketAddress localAddress) { this.localAddress = localAddress; return this; }
            public Builder localAddress(String host, int port) { this.localAddress = new InetSocketAddress(host, port); return this; }
            public Builder remoteAddress(InetSocketAddress remoteAddress) { this.remoteAddress = remoteAddress; return this; }
            public Builder remoteAddress(String host, int port) { this.remoteAddress = new InetSocketAddress(host, port); return this; }

            public Local build() {
                if (alias == null || alias.isBlank()) throw new IllegalArgumentException("Alias required");
                if (localAddress == null) throw new IllegalArgumentException("Local address required");
                if (remoteAddress == null) throw new IllegalArgumentException("Remote address required");
                if (connectionId == null) throw new IllegalArgumentException("Connection ID required");
                if (id == null) id = EntityId.generate("fwd", alias.hashCode() & Integer.MAX_VALUE);
                return new Local(id, connectionId, alias, localAddress, remoteAddress);
            }
        }
    }

    /**
     * Remote port forwarding (ssh -R).
     * Binds a remote port and forwards traffic back to local destination.
     */
    public static final class Remote extends Forward {
        private final InetSocketAddress remoteAddress;
        private final InetSocketAddress localAddress;

        private Remote(EntityId id, EntityId connectionId, String alias,
                       InetSocketAddress remoteAddress, InetSocketAddress localAddress) {
            super(id, connectionId, alias);
            this.remoteAddress = remoteAddress;
            this.localAddress = localAddress;
        }

        public InetSocketAddress remoteAddress() {
            return remoteAddress;
        }

        public InetSocketAddress localAddress() {
            return localAddress;
        }

        @Override
        public ForwardType type() {
            return ForwardType.REMOTE;
        }

        @Override
        public String toString() {
            return "RemoteForward[%s: %s -> %s]".formatted(alias(), remoteAddress, localAddress);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private EntityId id;
            private EntityId connectionId;
            private String alias;
            private InetSocketAddress remoteAddress;
            private InetSocketAddress localAddress;

            private Builder() {}

            public Builder id(EntityId id) { this.id = id; return this; }
            public Builder connectionId(EntityId connectionId) { this.connectionId = connectionId; return this; }
            public Builder alias(String alias) { this.alias = alias; return this; }
            public Builder remoteAddress(InetSocketAddress remoteAddress) { this.remoteAddress = remoteAddress; return this; }
            public Builder remoteAddress(String host, int port) { this.remoteAddress = new InetSocketAddress(host, port); return this; }
            public Builder localAddress(InetSocketAddress localAddress) { this.localAddress = localAddress; return this; }
            public Builder localAddress(String host, int port) { this.localAddress = new InetSocketAddress(host, port); return this; }

            public Remote build() {
                if (alias == null || alias.isBlank()) throw new IllegalArgumentException("Alias required");
                if (remoteAddress == null) throw new IllegalArgumentException("Remote address required");
                if (localAddress == null) throw new IllegalArgumentException("Local address required");
                if (connectionId == null) throw new IllegalArgumentException("Connection ID required");
                if (id == null) id = EntityId.generate("fwd", alias.hashCode() & Integer.MAX_VALUE);
                return new Remote(id, connectionId, alias, remoteAddress, localAddress);
            }
        }
    }

    /**
     * Dynamic port forwarding (ssh -D).
     * Acts as a SOCKS proxy.
     */
    public static final class Dynamic extends Forward {
        private final InetSocketAddress localAddress;

        private Dynamic(EntityId id, EntityId connectionId, String alias, InetSocketAddress localAddress) {
            super(id, connectionId, alias);
            this.localAddress = localAddress;
        }

        public InetSocketAddress localAddress() {
            return localAddress;
        }

        @Override
        public ForwardType type() {
            return ForwardType.DYNAMIC;
        }

        @Override
        public String toString() {
            return "DynamicForward[%s: %s (SOCKS)]".formatted(alias(), localAddress);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private EntityId id;
            private EntityId connectionId;
            private String alias;
            private InetSocketAddress localAddress;

            private Builder() {}

            public Builder id(EntityId id) { this.id = id; return this; }
            public Builder connectionId(EntityId connectionId) { this.connectionId = connectionId; return this; }
            public Builder alias(String alias) { this.alias = alias; return this; }
            public Builder localAddress(InetSocketAddress localAddress) { this.localAddress = localAddress; return this; }
            public Builder localAddress(String host, int port) { this.localAddress = new InetSocketAddress(host, port); return this; }

            public Dynamic build() {
                if (alias == null || alias.isBlank()) throw new IllegalArgumentException("Alias required");
                if (localAddress == null) throw new IllegalArgumentException("Local address required");
                if (connectionId == null) throw new IllegalArgumentException("Connection ID required");
                if (id == null) id = EntityId.generate("fwd", alias.hashCode() & Integer.MAX_VALUE);
                return new Dynamic(id, connectionId, alias, localAddress);
            }
        }
    }
}
