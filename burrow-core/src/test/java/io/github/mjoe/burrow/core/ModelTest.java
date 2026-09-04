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

import io.github.mjoe.burrow.core.config.Configuration;
import io.github.mjoe.burrow.core.model.*;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    void testEntityIdCreation() {
        var id = new EntityId("test-1");
        assertEquals("test-1", id.value());

        assertThrows(IllegalArgumentException.class, () -> new EntityId(null));
        assertThrows(IllegalArgumentException.class, () -> new EntityId(""));
        assertThrows(IllegalArgumentException.class, () -> new EntityId("  "));
    }

    @Test
    void testEntityIdGenerated() {
        var id = EntityId.generate("test", 42);
        assertEquals("test-42", id.value());
    }

    @Test
    void testIdentityBuilder() {
        var identity = Identity.builder()
                .alias("my-server")
                .username("admin")
                .password("secret")
                .build();

        assertEquals("my-server", identity.alias());
        assertEquals("admin", identity.username());
        assertTrue(identity.hasPassword());
        assertFalse(identity.hasKeyFile());
    }

    @Test
    void testIdentityWithKeyFile() {
        var identity = Identity.builder()
                .alias("key-server")
                .username("admin")
                .keyFilePath("/path/to/key")
                .keyPassphrase("passphrase")
                .build();

        assertTrue(identity.hasKeyFile());
        assertEquals("/path/to/key", identity.keyFilePath().orElse(null));
        assertEquals("passphrase", identity.keyPassphrase().orElse(null));
    }

    @Test
    void testIdentityValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                Identity.builder().username("admin").build());

        assertThrows(IllegalArgumentException.class, () ->
                Identity.builder().alias("test").build());
    }

    @Test
    void testConnectionBuilder() {
        var identityId = EntityId.generate("id", 1);
        var connection = Connection.builder()
                .alias("my-connection")
                .host("example.com")
                .port(22)
                .identityId(identityId)
                .autoStart(true)
                .autoReconnect(false)
                .build();

        assertEquals("my-connection", connection.alias());
        assertEquals("example.com", connection.host());
        assertEquals(22, connection.port());
        assertEquals(identityId, connection.identityId());
        assertTrue(connection.autoStart());
        assertFalse(connection.autoReconnect());
    }

    @Test
    void testConnectionValidation() {
        var identityId = EntityId.generate("id", 1);

        assertThrows(IllegalArgumentException.class, () ->
                Connection.builder().host("example.com").identityId(identityId).build());

        assertThrows(IllegalArgumentException.class, () ->
                Connection.builder().alias("test").identityId(identityId).build());

        assertThrows(IllegalArgumentException.class, () ->
                Connection.builder().alias("test").host("example.com").build());
    }

    @Test
    void testLocalForward() {
        var connectionId = EntityId.generate("conn", 1);
        var forward = Forward.Local.builder()
                .alias("my-forward")
                .connectionId(connectionId)
                .localAddress("127.0.0.1", 8080)
                .remoteAddress("10.0.0.1", 80)
                .build();

        assertEquals(Forward.ForwardType.LOCAL, forward.type());
        assertEquals(new InetSocketAddress("127.0.0.1", 8080), forward.localAddress());
        assertEquals(new InetSocketAddress("10.0.0.1", 80), forward.remoteAddress());
    }

    @Test
    void testRemoteForward() {
        var connectionId = EntityId.generate("conn", 1);
        var forward = Forward.Remote.builder()
                .alias("remote-forward")
                .connectionId(connectionId)
                .remoteAddress("0.0.0.0", 3000)
                .localAddress("127.0.0.1", 3000)
                .build();

        assertEquals(Forward.ForwardType.REMOTE, forward.type());
        assertEquals(new InetSocketAddress("0.0.0.0", 3000), forward.remoteAddress());
        assertEquals(new InetSocketAddress("127.0.0.1", 3000), forward.localAddress());
    }

    @Test
    void testDynamicForward() {
        var connectionId = EntityId.generate("conn", 1);
        var forward = Forward.Dynamic.builder()
                .alias("socks-proxy")
                .connectionId(connectionId)
                .localAddress("127.0.0.1", 1080)
                .build();

        assertEquals(Forward.ForwardType.DYNAMIC, forward.type());
        assertEquals(new InetSocketAddress("127.0.0.1", 1080), forward.localAddress());
    }

    @Test
    void testConnectionStatus() {
        assertEquals("Disconnected", ConnectionStatus.DISCONNECTED.displayName());
        assertEquals("Connected", ConnectionStatus.CONNECTED.displayName());
        assertTrue(ConnectionStatus.CONNECTED.isActive());
        assertFalse(ConnectionStatus.DISCONNECTED.isActive());
    }

    @Test
    void testConfiguration() {
        var identity = Identity.builder()
                .alias("test")
                .username("admin")
                .build();

        var connection = Connection.builder()
                .alias("test-conn")
                .host("example.com")
                .identityId(identity.id())
                .build();

        var config = Configuration.builder()
                .identity(identity)
                .connection(connection)
                .build();

        assertEquals(1, config.identities().size());
        assertEquals(1, config.connections().size());
        assertEquals(0, config.forwards().size());
    }
}
