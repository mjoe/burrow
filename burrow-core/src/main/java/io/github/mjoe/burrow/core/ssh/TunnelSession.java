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

package io.github.mjoe.burrow.core.ssh;

import io.github.mjoe.burrow.core.model.Connection;
import io.github.mjoe.burrow.core.model.Forward;
import io.github.mjoe.burrow.core.model.Identity;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages a single SSH session and its port forwards.
 */
final class TunnelSession {
    private static final Logger log = LoggerFactory.getLogger(TunnelSession.class);

    private final Connection connection;
    private final Identity identity;
    private final List<Forward> forwards;
    private final CopyOnWriteArrayList<Object> trackers;

    private SshClient client;
    private ClientSession session;

    TunnelSession(Connection connection, Identity identity) {
        this.connection = connection;
        this.identity = identity;
        this.forwards = new ArrayList<>();
        this.trackers = new CopyOnWriteArrayList<>();
    }

    void connect() throws IOException {
        client = SshClient.setUpDefaultClient();
        client.start();

        session = client.connect(identity.username(), connection.host(), connection.port())
                .verify(30000)
                .getSession();

        // Authenticate
        if (identity.hasPassword()) {
            identity.password().ifPresent(session::addPasswordIdentity);
        }

        if (identity.hasKeyFile()) {
            identity.keyFilePath().ifPresent(keyFile -> {
                try {
                    var keyProvider = new FileKeyPairProvider(Path.of(keyFile));
                    var keyPairs = keyProvider.loadKeys(session);
                    for (var keyPair : keyPairs) {
                        session.addPublicKeyIdentity(keyPair);
                    }
                } catch (Exception e) {
                    log.error("Failed to load key file: {}", keyFile, e);
                }
            });
        }

        session.auth().verify(30000);
        log.info("SSH session established to {}:{}", connection.host(), connection.port());
    }

    void addForward(Forward forward) throws IOException {
        forwards.add(forward);

        switch (forward) {
            case Forward.Local local -> {
                var bindAddress = new SshdSocketAddress(
                        local.localAddress().getHostString(),
                        local.localAddress().getPort());
                var remoteAddress = new SshdSocketAddress(
                        local.remoteAddress().getHostString(),
                        local.remoteAddress().getPort());

                var tracker = session.createLocalPortForwardingTracker(bindAddress, remoteAddress);
                trackers.add(tracker);
                log.info("Local forward established: {} -> {}", local.localAddress(), local.remoteAddress());
            }
            case Forward.Remote remote -> {
                var bindAddress = new SshdSocketAddress(
                        remote.remoteAddress().getHostString(),
                        remote.remoteAddress().getPort());
                var localAddress = new SshdSocketAddress(
                        remote.localAddress().getHostString(),
                        remote.localAddress().getPort());

                var tracker = session.createRemotePortForwardingTracker(bindAddress, localAddress);
                trackers.add(tracker);
                log.info("Remote forward established: {} -> {}", remote.remoteAddress(), remote.localAddress());
            }
            case Forward.Dynamic dynamic -> {
                var bindAddress = new SshdSocketAddress(
                        dynamic.localAddress().getHostString(),
                        dynamic.localAddress().getPort());

                var tracker = session.createDynamicPortForwardingTracker(bindAddress);
                trackers.add(tracker);
                log.info("Dynamic forward (SOCKS) established: {}", dynamic.localAddress());
            }
        }
    }

    void disconnect() throws IOException {
        // Close all trackers
        for (var tracker : trackers) {
            try {
                if (tracker instanceof AutoCloseable closeable) {
                    closeable.close();
                }
            } catch (Exception e) {
                log.debug("Error closing tracker", e);
            }
        }
        trackers.clear();

        // Close session
        if (session != null) {
            session.close();
            session = null;
        }

        // Stop client
        if (client != null) {
            client.stop();
            client = null;
        }

        log.info("SSH session disconnected");
    }

    boolean isConnected() {
        return session != null && session.isOpen();
    }

    Connection getConnection() {
        return connection;
    }
}
