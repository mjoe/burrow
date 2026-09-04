<div align="center">

# Burrow

**SSH Tunnel Management · Made Easy**

[![License](https://img.shields.io/badge/license-Apache%202.0-informational.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-25-blue.svg?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Build](https://github.com/mjoe/burrow/actions/workflows/ci.yml/badge.svg)](https://github.com/mjoe/burrow/actions/workflows/ci.yml)

A modern, secure SSH tunnel manager with a clean Swing GUI.

Build, manage and auto-reconnect local, remote and dynamic port forwarding
tunnels through an intuitive desktop interface.

</div>

---

## Features

| Feature                        | Description                                                     |
| ------------------------------ | --------------------------------------------------------------- |
| **Port Forwarding**            | Local, remote and dynamic (SOCKS) tunneling                      |
| **Auto-Reconnect**             | Resilient reconnection with exponential backoff                  |
| **Secure Credentials**         | AES-GCM encryption with a persisted, owner-only key file         |
| **YAML Configuration**         | Human-readable, declarative config                              |
| **Modern GUI**                 | FlatLaf theming with dark & light modes                          |
| **Identity Management**        | Reusable SSH identities with password or key authentication      |

## Quick Start

```bash
# 1. Clone
git clone https://github.com/mjoe/burrow.git
cd burrow

# 2. Build (uses Maven Wrapper - no manual Maven install needed)
./mvnw clean package

# 3. Run
java -jar burrow-gui/target/burrow-gui-<version>.jar
```

### Requirements

- **Java 25+** — [Eclipse Temurin](https://adoptium.net/)

The bundled [Maven Wrapper](https://maven.apache.org/wrapper/) (`./mvnw`)
downloads the required Maven version automatically. You only need a system
Maven if you prefer using `mvn` directly.

## Configuration

Burrow reads its configuration from `~/.burrow/config.yaml`. You can also
load a file at runtime via the GUI (`File → Load Configuration`).

```yaml
identities:
  - id: id-1
    alias: My Server
    username: admin
    keyFile: /home/user/.ssh/id_ed25519

connections:
  - id: conn-1
    alias: Home Server
    host: home.example.com
    port: 22
    identityId: id-1
    autoStart: true
    autoReconnect: true

forwards:
  - id: fwd-1
    type: local
    alias: Web App
    connectionId: conn-1
    localHost: 127.0.0.1
    localPort: 8080
    remoteHost: 127.0.0.1
    remotePort: 80
```

> **Note:** Stored passwords are encrypted with an AES key persisted at
> `~/.burrow/secret.key` (owner-only permissions). Never share this file —
> without it, stored passwords cannot be decrypted.

## Architecture

Burrow is a multi-module Maven project:

| Module        | Responsibility                                        |
| ------------- | ----------------------------------------------------- |
| `burrow-core` | Domain models, SSH logic, configuration, security     |
| `burrow-gui`  | Swing desktop application                             |

**Design principles:**

- Immutable records with builder patterns for type-safe models
- Sealed hierarchies for well-defined port-forwarding types
- Core logic cleanly separated from the GUI layer
- Secure credentials by default, never hardcoded keys

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md)
before submitting issues or pull requests.

## License

[Apache License, Version 2.0](LICENSE)

## Credits

- Original project: [jentunnel](https://github.com/ggrandes/jentunnel) by ggrandes
- SSH: [Apache Mina SSHD](https://github.com/apache/mina-sshd/)
- GUI: [FlatLaf](https://www.formdev.com/flatlaf/)
- Inspired by [MyEnTunnel](https://web.archive.org/web/20161029055944/http://nemesis2.qx.net/pages/MyEnTunnel) and [PuTTY](https://www.chiark.greenend.org.uk/~sgtatham/putty/)
