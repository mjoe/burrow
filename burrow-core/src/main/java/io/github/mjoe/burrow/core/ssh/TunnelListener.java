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

/**
 * Listener for tunnel events.
 */
public interface TunnelListener {
    void onConnecting(String connectionAlias);
    void onConnected(String connectionAlias);
    void onDisconnected(String connectionAlias, String reason);
    void onFailed(String connectionAlias, String reason);
    void onReconnecting(String connectionAlias, int attempt);
    void onForwardEstablished(String forwardAlias, String type);
    void onForwardClosed(String forwardAlias, String type);
}
