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

import io.github.mjoe.burrow.core.config.Configuration;
import io.github.mjoe.burrow.core.config.ConfigurationLoader;
import io.github.mjoe.burrow.core.model.Connection;
import io.github.mjoe.burrow.core.model.ConnectionStatus;
import io.github.mjoe.burrow.core.model.Forward;
import io.github.mjoe.burrow.core.model.Identity;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main SSH tunnel manager.
 * Manages multiple SSH connections and their port forwards.
 */
public final class TunnelManager {
    private static final Logger log = LoggerFactory.getLogger(TunnelManager.class);

    private static final int MIN_RECONNECT_DELAY_MS = 1000;
    private static final int MAX_RECONNECT_DELAY_MS = 30000;

    private final ConfigurationLoader configLoader;
    private final ScheduledExecutorService scheduler;
    private final CopyOnWriteArrayList<TunnelListener> listeners;
    private final Map<String, TunnelSession> sessions;
    private final Map<String, ConnectionStatus> statuses;
    private final AtomicInteger reconnectCounter;

    private Configuration configuration;
    private Path configPath;

    public TunnelManager() {
        this.configLoader = new ConfigurationLoader();
        this.reconnectCounter = new AtomicInteger(0);
        this.scheduler = Executors.newScheduledThreadPool(4, r -> {
            var thread = new Thread(r, "burrow-scheduler-" + reconnectCounter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
        this.listeners = new CopyOnWriteArrayList<>();
        this.sessions = new ConcurrentHashMap<>();
        this.statuses = new ConcurrentHashMap<>();
        this.configuration = Configuration.builder().build();
    }

    public void addListener(TunnelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TunnelListener listener) {
        listeners.remove(listener);
    }

    public void loadConfiguration(Path path) throws IOException {
        this.configPath = path;
        this.configuration = configLoader.load(path);
        log.info("Configuration loaded: {} identities, {} connections, {} forwards",
                configuration.identities().size(),
                configuration.connections().size(),
                configuration.forwards().size());
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void saveConfiguration() throws IOException {
        if (configPath != null) {
            configLoader.save(configuration, configPath);
            log.info("Configuration saved");
        }
    }

    public ConnectionStatus getStatus(String connectionId) {
        return statuses.getOrDefault(connectionId, ConnectionStatus.DISCONNECTED);
    }

    public Map<String, ConnectionStatus> getAllStatuses() {
        return Map.copyOf(statuses);
    }

    public void connect(String connectionId) {
        var connection = findConnection(connectionId);
        if (connection == null) {
            log.warn("Connection not found: {}", connectionId);
            return;
        }

        var identity = findIdentity(connection.identityId().value());
        if (identity == null) {
            log.warn("Identity not found for connection: {}", connectionId);
            return;
        }

        if (statuses.get(connectionId) == ConnectionStatus.CONNECTED) {
            log.info("Already connected: {}", connection.alias());
            return;
        }

        updateStatus(connectionId, ConnectionStatus.CONNECTING);
        notifyConnecting(connection.alias());

        var session = new TunnelSession(connection, identity);
        sessions.put(connectionId, session);

        scheduler.submit(() -> {
            try {
                session.connect();
                updateStatus(connectionId, ConnectionStatus.CONNECTED);
                notifyConnected(connection.alias());

                // Start forwards
                var forwards = findForwards(connectionId);
                for (var forward : forwards) {
                    session.addForward(forward);
                    notifyForwardEstablished(forward.alias(), forward.type().name());
                }

                log.info("Connected to {} ({})", connection.alias(), connection.host());
            } catch (IOException e) {
                log.error("Failed to connect to {}: {}", connection.alias(), e.getMessage());
                updateStatus(connectionId, ConnectionStatus.FAILED);
                notifyFailed(connection.alias(), e.getMessage());

                if (connection.autoReconnect()) {
                    scheduleReconnect(connection, identity, 1);
                }
            }
        });
    }

    public void disconnect(String connectionId) {
        var session = sessions.remove(connectionId);
        if (session != null) {
            scheduler.submit(() -> {
                try {
                    session.disconnect();
                } catch (IOException e) {
                    log.warn("Error disconnecting: {}", e.getMessage());
                }
            });
        }
        updateStatus(connectionId, ConnectionStatus.DISCONNECTED);
    }

    public void connectAll() {
        for (var connection : configuration.connections()) {
            if (connection.autoStart()) {
                connect(connection.id().value());
            }
        }
    }

    public void disconnectAll() {
        for (var connectionId : sessions.keySet().toArray(new String[0])) {
            disconnect(connectionId);
        }
    }

    public void shutdown() {
        log.info("Shutting down tunnel manager");
        disconnectAll();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void scheduleReconnect(Connection connection, Identity identity, int attempt) {
        var delay = Math.min(
                MIN_RECONNECT_DELAY_MS * (1L << Math.min(attempt - 1, 10)),
                MAX_RECONNECT_DELAY_MS
        );

        log.info("Scheduling reconnect for {} in {}ms (attempt {})", connection.alias(), delay, attempt);
        notifyReconnecting(connection.alias(), attempt);

        scheduler.schedule(() -> {
            if (statuses.get(connection.id().value()) != ConnectionStatus.DISCONNECTED) {
                return; // Already connected or manually disconnected
            }

            updateStatus(connection.id().value(), ConnectionStatus.RECONNECTING);

            var session = new TunnelSession(connection, identity);
            sessions.put(connection.id().value(), session);

            try {
                session.connect();
                updateStatus(connection.id().value(), ConnectionStatus.CONNECTED);
                notifyConnected(connection.alias());

                var forwards = findForwards(connection.id().value());
                for (var forward : forwards) {
                    session.addForward(forward);
                    notifyForwardEstablished(forward.alias(), forward.type().name());
                }
            } catch (IOException e) {
                log.warn("Reconnect failed for {}: {}", connection.alias(), e.getMessage());
                updateStatus(connection.id().value(), ConnectionStatus.FAILED);
                notifyFailed(connection.alias(), e.getMessage());

                if (connection.autoReconnect()) {
                    scheduleReconnect(connection, identity, attempt + 1);
                }
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private Connection findConnection(String id) {
        return configuration.connections().stream()
                .filter(c -> c.id().value().equals(id))
                .findFirst()
                .orElse(null);
    }

    private Identity findIdentity(String id) {
        return configuration.identities().stream()
                .filter(i -> i.id().value().equals(id))
                .findFirst()
                .orElse(null);
    }

    private java.util.List<Forward> findForwards(String connectionId) {
        return configuration.forwards().stream()
                .filter(f -> f.connectionId().value().equals(connectionId))
                .toList();
    }

    private void updateStatus(String connectionId, ConnectionStatus status) {
        statuses.put(connectionId, status);
    }

    private void notifyConnecting(String alias) {
        for (var listener : listeners) {
            try {
                listener.onConnecting(alias);
            } catch (Exception e) {
                log.warn("Error in listener", e);
            }
        }
    }

    private void notifyConnected(String alias) {
        for (var listener : listeners) {
            try {
                listener.onConnected(alias);
            } catch (Exception e) {
                log.warn("Error in listener", e);
            }
        }
    }

    private void notifyDisconnected(String alias, String reason) {
        for (var listener : listeners) {
            try {
                listener.onDisconnected(alias, reason);
            } catch (Exception e) {
                log.warn("Error in listener", e);
            }
        }
    }

    private void notifyFailed(String alias, String reason) {
        for (var listener : listeners) {
            try {
                listener.onFailed(alias, reason);
            } catch (Exception e) {
                log.warn("Error in listener", e);
            }
        }
    }

    private void notifyReconnecting(String alias, int attempt) {
        for (var listener : listeners) {
            try {
                listener.onReconnecting(alias, attempt);
            } catch (Exception e) {
                log.warn("Error in listener", e);
            }
        }
    }

    private void notifyForwardEstablished(String alias, String type) {
        for (var listener : listeners) {
            try {
                listener.onForwardEstablished(alias, type);
            } catch (Exception e) {
                log.warn("Error in listener", e);
            }
        }
    }

    private void notifyForwardClosed(String alias, String type) {
        for (var listener : listeners) {
            try {
                listener.onForwardClosed(alias, type);
            } catch (Exception e) {
                log.warn("Error in listener", e);
            }
        }
    }
}
