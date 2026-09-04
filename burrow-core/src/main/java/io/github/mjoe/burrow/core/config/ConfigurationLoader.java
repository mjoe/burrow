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
import io.github.mjoe.burrow.core.model.EntityId;
import io.github.mjoe.burrow.core.model.Forward;
import io.github.mjoe.burrow.core.model.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads and saves configuration in YAML format.
 */
public final class ConfigurationLoader {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationLoader.class);

    private static final String TAG_CONFIG = "!config";
    private static final String TAG_IDENTITY = "!identity";
    private static final String TAG_CONNECTION = "!connection";
    private static final String TAG_LOCAL_FORWARD = "!local";
    private static final String TAG_REMOTE_FORWARD = "!remote";
    private static final String TAG_DYNAMIC_FORWARD = "!dynamic";

    private final Yaml yaml;

    public ConfigurationLoader() {
        this.yaml = createYaml();
    }

    private Yaml createYaml() {
        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        return new Yaml(options);
    }

    public Configuration load(Path path) throws IOException {
        if (!Files.exists(path)) {
            log.info("Configuration file not found at {}, using empty configuration", path);
            return Configuration.builder().build();
        }

        log.info("Loading configuration from {}", path);
        var content = Files.readString(path);
        return parse(content);
    }

    @SuppressWarnings("unchecked")
    public Configuration parse(String content) {
        var builder = Configuration.builder();

        var data = yaml.load(content);
        if (data == null) {
            return builder.build();
        }

        if (data instanceof Map<?, ?> root) {
            parseIdentities(root, builder);
            parseConnections(root, builder);
            parseForwards(root, builder);
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private void parseIdentities(Map<?, ?> root, Configuration.Builder builder) {
        var identities = root.get("identities");
        if (identities instanceof List<?> list) {
            for (var item : list) {
                if (item instanceof Map<?, ?> map) {
                    builder.identity(parseIdentity(map));
                }
            }
        }
    }

    private Identity parseIdentity(Map<?, ?> map) {
        return Identity.builder()
                .id(getEntityId(map, "id"))
                .alias(getString(map, "alias", "unnamed"))
                .username(getString(map, "username", ""))
                .password(getString(map, "password", null))
                .keyFilePath(getString(map, "keyFile", null))
                .keyPassphrase(getString(map, "keyPassphrase", null))
                .build();
    }

    @SuppressWarnings("unchecked")
    private void parseConnections(Map<?, ?> root, Configuration.Builder builder) {
        var connections = root.get("connections");
        if (connections instanceof List<?> list) {
            for (var item : list) {
                if (item instanceof Map<?, ?> map) {
                    builder.connection(parseConnection(map));
                }
            }
        }
    }

    private Connection parseConnection(Map<?, ?> map) {
        return Connection.builder()
                .id(getEntityId(map, "id"))
                .alias(getString(map, "alias", "unnamed"))
                .host(getString(map, "host", "localhost"))
                .port(getInt(map, "port", 22))
                .identityId(getEntityId(map, "identityId"))
                .autoStart(getBoolean(map, "autoStart", false))
                .autoReconnect(getBoolean(map, "autoReconnect", true))
                .build();
    }

    @SuppressWarnings("unchecked")
    private void parseForwards(Map<?, ?> root, Configuration.Builder builder) {
        var forwards = root.get("forwards");
        if (forwards instanceof List<?> list) {
            for (var item : list) {
                if (item instanceof Map<?, ?> map) {
                    var type = getString(map, "type", "local");
                    var forward = switch (type.toLowerCase()) {
                        case "remote" -> parseRemoteForward(map);
                        case "dynamic" -> parseDynamicForward(map);
                        default -> parseLocalForward(map);
                    };
                    builder.forward(forward);
                }
            }
        }
    }

    private Forward.Local parseLocalForward(Map<?, ?> map) {
        return Forward.Local.builder()
                .id(getEntityId(map, "id"))
                .connectionId(getEntityId(map, "connectionId"))
                .alias(getString(map, "alias", "unnamed"))
                .localAddress(getString(map, "localHost", "127.0.0.1"), getInt(map, "localPort", 0))
                .remoteAddress(getString(map, "remoteHost", "127.0.0.1"), getInt(map, "remotePort", 0))
                .build();
    }

    private Forward.Remote parseRemoteForward(Map<?, ?> map) {
        return Forward.Remote.builder()
                .id(getEntityId(map, "id"))
                .connectionId(getEntityId(map, "connectionId"))
                .alias(getString(map, "alias", "unnamed"))
                .remoteAddress(getString(map, "remoteHost", "127.0.0.1"), getInt(map, "remotePort", 0))
                .localAddress(getString(map, "localHost", "127.0.0.1"), getInt(map, "localPort", 0))
                .build();
    }

    private Forward.Dynamic parseDynamicForward(Map<?, ?> map) {
        return Forward.Dynamic.builder()
                .id(getEntityId(map, "id"))
                .connectionId(getEntityId(map, "connectionId"))
                .alias(getString(map, "alias", "unnamed"))
                .localAddress(getString(map, "localHost", "127.0.0.1"), getInt(map, "localPort", 0))
                .build();
    }

    public void save(Configuration config, Path path) throws IOException {
        var parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        var content = dump(config);
        Files.writeString(path, content);
        log.info("Configuration saved to {}", path);
    }

    public String dump(Configuration config) {
        var root = new ArrayList<>();

        // Identities
        var identityMaps = new ArrayList<>();
        for (var identity : config.identities()) {
            var map = new java.util.LinkedHashMap<String, Object>();
            map.put("tag", TAG_IDENTITY);
            map.put("id", identity.id().value());
            map.put("alias", identity.alias());
            map.put("username", identity.username());
            identity.password().ifPresent(p -> map.put("password", p));
            identity.keyFilePath().ifPresent(k -> map.put("keyFile", k));
            identity.keyPassphrase().ifPresent(p -> map.put("keyPassphrase", p));
            identityMaps.add(map);
        }

        // Connections
        var connectionMaps = new ArrayList<>();
        for (var connection : config.connections()) {
            var map = new java.util.LinkedHashMap<String, Object>();
            map.put("tag", TAG_CONNECTION);
            map.put("id", connection.id().value());
            map.put("alias", connection.alias());
            map.put("host", connection.host());
            map.put("port", connection.port());
            map.put("identityId", connection.identityId().value());
            map.put("autoStart", connection.autoStart());
            map.put("autoReconnect", connection.autoReconnect());
            connectionMaps.add(map);
        }

        // Forwards
        var forwardMaps = new ArrayList<>();
        for (var forward : config.forwards()) {
            var map = new java.util.LinkedHashMap<String, Object>();
            map.put("tag", switch (forward.type()) {
                case LOCAL -> TAG_LOCAL_FORWARD;
                case REMOTE -> TAG_REMOTE_FORWARD;
                case DYNAMIC -> TAG_DYNAMIC_FORWARD;
            });
            map.put("id", forward.id().value());
            map.put("connectionId", forward.connectionId().value());
            map.put("alias", forward.alias());

            switch (forward) {
                case Forward.Local local -> {
                    map.put("localHost", local.localAddress().getHostString());
                    map.put("localPort", local.localAddress().getPort());
                    map.put("remoteHost", local.remoteAddress().getHostString());
                    map.put("remotePort", local.remoteAddress().getPort());
                }
                case Forward.Remote remote -> {
                    map.put("remoteHost", remote.remoteAddress().getHostString());
                    map.put("remotePort", remote.remoteAddress().getPort());
                    map.put("localHost", remote.localAddress().getHostString());
                    map.put("localPort", remote.localAddress().getPort());
                }
                case Forward.Dynamic dynamic -> {
                    map.put("localHost", dynamic.localAddress().getHostString());
                    map.put("localPort", dynamic.localAddress().getPort());
                }
            }

            forwardMaps.add(map);
        }

        // Build YAML structure
        var rootMap = new java.util.LinkedHashMap<String, Object>();
        rootMap.put("identities", identityMaps);
        rootMap.put("connections", connectionMaps);
        rootMap.put("forwards", forwardMaps);

        return yaml.dump(rootMap);
    }

    // Utility methods

    private EntityId getEntityId(Map<?, ?> map, String key) {
        var value = map.get(key);
        if (value instanceof String s) {
            return new EntityId(s);
        }
        return EntityId.generate("auto", (int) (System.nanoTime() & Integer.MAX_VALUE));
    }

    private String getString(Map<?, ?> map, String key, String defaultValue) {
        var value = map.get(key);
        if (value instanceof String s) {
            return s;
        }
        return defaultValue;
    }

    private int getInt(Map<?, ?> map, String key, int defaultValue) {
        var value = map.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        return defaultValue;
    }

    private boolean getBoolean(Map<?, ?> map, String key, boolean defaultValue) {
        var value = map.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        return defaultValue;
    }
}
