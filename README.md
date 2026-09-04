# Burrow

SSH Tunnel Management - Made Easy

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-25+-blue.svg)](https://jdk.java.net/)
[![Maven](https://img.shields.io/badge/maven-3.9+-green.svg)](https://maven.apache.org/)

A modern SSH tunnel management tool with a clean Swing GUI. Forked and redesigned from [jentunnel](https://github.com/ggrandes/jentunnel).

## Features

- **SSH Tunnel Management**: Create, edit, and delete SSH tunnels
- **Port Forwarding**: Support for local, remote, and dynamic (SOCKS) forwarding
- **Auto-Reconnect**: Automatic reconnection with exponential backoff
- **Modern GUI**: Clean Swing interface with FlatLaf theming
- **YAML Configuration**: Simple, human-readable configuration files
- **Secure Password Storage**: AES-GCM encryption for stored credentials

## Requirements

- Java 25 or later: [Download JDK](https://jdk.java.net/)
- Maven 3.9 or later (for building)

## Building

```bash
# Clone the repository
git clone https://github.com/mjoe/burrow.git
cd burrow

# Build the project
mvn clean package

# Run the application
java -jar burrow-gui/target/burrow-gui-2.0.0-SNAPSHOT.jar
```

## Configuration

Burrow uses YAML configuration files. See `sample-config.yaml` for an example.

### Configuration Structure

```yaml
identities:
  - id: id-1
    alias: My Server
    username: admin
    keyFile: ~/.ssh/id_ed25519

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

## Architecture

Burrow is organized as a multi-module Maven project:

- **burrow-core**: Core library with SSH logic, configuration, and domain models
- **burrow-gui**: Swing GUI application

### Key Improvements Over jentunnel

1. **Modern Java**: Uses Java 21 features (records, sealed classes, pattern matching)
2. **Clean Architecture**: Separated core logic from GUI
3. **Type-Safe Models**: Immutable records with builder pattern
4. **Secure Credentials**: AES-GCM encryption instead of obfuscation
5. **No Hardcoded Keys**: Encryption keys stored separately
6. **Comprehensive Tests**: Unit tests for core functionality
7. **Modern Dependencies**: Up-to-date libraries (SSH, crypto, GUI)

## Development

### Prerequisites

- JDK 25+
- Maven 3.9+

### Running Tests

```bash
mvn test
```

### IDE Setup

Import as a Maven project. Recommended IDEs:
- IntelliJ IDEA
- Eclipse with Maven plugin
- VS Code with Java extensions

## License

Apache License, Version 2.0 - see [LICENSE](LICENSE) for details.

## Credits

- Original project: [jentunnel](https://github.com/ggrandes/jentunnel) by ggrandes
- SSH library: [Apache Mina SSHD](https://github.com/apache/mina-sshd/)
- GUI theming: [FlatLaf](https://www.formdev.com/flatlaf/)
- Inspired by: [MyEnTunnel](https://web.archive.org/web/20161029055944/http://nemesis2.qx.net/pages/MyEnTunnel) and [PuTTY](https://www.chiark.greenend.org.uk/~sgtatham/putty/)
