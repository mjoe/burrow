# AGENTS.md

Guidance for AI coding assistants working in this repository.

## Project

Burrow is a multi-module Maven project for SSH tunnel management (Swing GUI).

- `burrow-core` - domain models, SSH logic, configuration, security
- `burrow-gui` - Swing desktop application

## Build & Test

- Always use the Maven Wrapper: `./mvnw` (never bare `mvn`).
- Full build + tests: `./mvnw verify` (also runs the shade/fat-jar packaging).
- Run only core tests: `./mvnw test -pl burrow-core`.
- Java target is **25** (Eclipse Temurin). Do not downgrade.

## Conventions

- Java 25 features (records, sealed classes, pattern matching) are preferred.
- Immutable, type-safe models with builder patterns.
- Core logic stays in `burrow-core`; GUI/Swing code in `burrow-gui`.
- Add license headers to new Java files (Apache 2.0, copyright 2026).
- No comments unless the code genuinely needs explanation.
- Never hardcode credentials or encryption keys. Passwords use the AES-GCM
  `PasswordEncoder`; the key is persisted via `SecretKeyStore`.
- Keep exception signatures narrow (`GeneralSecurityException`, `IOException`)
  instead of broad `Exception`.

## Repository files

- `CONTRIBUTING.md` - contribution + AI-use policy for human contributors.
- `LICENSE` - Apache 2.0.
- `README.md` - user-facing documentation (keep marketing-free).
