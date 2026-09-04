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

package io.github.mjoe.burrow.core.config;

import io.github.mjoe.burrow.core.model.Connection;
import io.github.mjoe.burrow.core.model.Forward;
import io.github.mjoe.burrow.core.model.Identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Root configuration for Burrow.
 */
public final class Configuration {
    private final List<Identity> identities;
    private final List<Connection> connections;
    private final List<Forward> forwards;

    private Configuration(Builder builder) {
        this.identities = Collections.unmodifiableList(new ArrayList<>(builder.identities));
        this.connections = Collections.unmodifiableList(new ArrayList<>(builder.connections));
        this.forwards = Collections.unmodifiableList(new ArrayList<>(builder.forwards));
    }

    public List<Identity> identities() {
        return identities;
    }

    public List<Connection> connections() {
        return connections;
    }

    public List<Forward> forwards() {
        return forwards;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Identity> identities = new ArrayList<>();
        private final List<Connection> connections = new ArrayList<>();
        private final List<Forward> forwards = new ArrayList<>();

        private Builder() {}

        public Builder identities(List<Identity> identities) {
            this.identities.addAll(identities);
            return this;
        }

        public Builder identity(Identity identity) {
            this.identities.add(identity);
            return this;
        }

        public Builder connections(List<Connection> connections) {
            this.connections.addAll(connections);
            return this;
        }

        public Builder connection(Connection connection) {
            this.connections.add(connection);
            return this;
        }

        public Builder forwards(List<Forward> forwards) {
            this.forwards.addAll(forwards);
            return this;
        }

        public Builder forward(Forward forward) {
            this.forwards.add(forward);
            return this;
        }

        public Configuration build() {
            return new Configuration(this);
        }
    }
}
