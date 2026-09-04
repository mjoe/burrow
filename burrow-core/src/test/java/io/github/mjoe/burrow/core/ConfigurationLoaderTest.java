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

import io.github.mjoe.burrow.core.config.ConfigurationLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationLoaderTest {

    private final ConfigurationLoader loader = new ConfigurationLoader();

    @Test
    void testLoadNonExistentFile(@TempDir Path tempDir) throws Exception {
        var config = loader.load(tempDir.resolve("nonexistent.yaml"));
        assertNotNull(config);
        assertTrue(config.identities().isEmpty());
        assertTrue(config.connections().isEmpty());
        assertTrue(config.forwards().isEmpty());
    }

    @Test
    void testParseEmptyYaml() {
        var config = loader.parse("");
        assertNotNull(config);
        assertTrue(config.identities().isEmpty());
    }

    @Test
    void testParseYamlWithIdentities() {
        var yaml = """
                identities:
                  - alias: my-server
                    username: admin
                    password: secret123
                """;

        var config = loader.parse(yaml);
        assertEquals(1, config.identities().size());

        var identity = config.identities().getFirst();
        assertEquals("my-server", identity.alias());
        assertEquals("admin", identity.username());
        assertTrue(identity.hasPassword());
    }

    @Test
    void testParseYamlWithConnections() {
        var yaml = """
                connections:
                  - alias: production
                    host: example.com
                    port: 22
                    identityId: id-1
                    autoStart: true
                    autoReconnect: false
                """;

        var config = loader.parse(yaml);
        assertEquals(1, config.connections().size());

        var connection = config.connections().getFirst();
        assertEquals("production", connection.alias());
        assertEquals("example.com", connection.host());
        assertEquals(22, connection.port());
        assertTrue(connection.autoStart());
        assertFalse(connection.autoReconnect());
    }

    @Test
    void testParseYamlWithForwards() {
        var yaml = """
                forwards:
                  - type: local
                    alias: web-forward
                    connectionId: conn-1
                    localHost: 127.0.0.1
                    localPort: 8080
                    remoteHost: 10.0.0.1
                    remotePort: 80
                  - type: dynamic
                    alias: socks-proxy
                    connectionId: conn-1
                    localHost: 127.0.0.1
                    localPort: 1080
                """;

        var config = loader.parse(yaml);
        assertEquals(2, config.forwards().size());
    }

    @Test
    void testSaveAndLoad(@TempDir Path tempDir) throws Exception {
        var configPath = tempDir.resolve("config.yaml");

        var config = io.github.mjoe.burrow.core.config.Configuration.builder()
                .identity(io.github.mjoe.burrow.core.model.Identity.builder()
                        .alias("test")
                        .username("admin")
                        .build())
                .build();

        loader.save(config, configPath);
        assertTrue(configPath.toFile().exists());

        var loaded = loader.load(configPath);
        assertEquals(1, loaded.identities().size());
        assertEquals("test", loaded.identities().getFirst().alias());
    }

    @Test
    void testDumpConfiguration() {
        var config = io.github.mjoe.burrow.core.config.Configuration.builder()
                .identity(io.github.mjoe.burrow.core.model.Identity.builder()
                        .alias("test")
                        .username("admin")
                        .build())
                .build();

        var yaml = loader.dump(config);
        assertNotNull(yaml);
        assertTrue(yaml.contains("test"));
        assertTrue(yaml.contains("admin"));
    }
}
