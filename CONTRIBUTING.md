# Contributing to Burrow

Thanks for considering contributing to Burrow! This document outlines the
guidelines for reporting issues, suggesting features, and submitting code.

## Getting Started

1. **Fork** the repository and create a branch from `master`.
2. Set up your environment with **Java 25+** ([Eclipse Temurin](https://adoptium.net/)).
   The bundled [Maven Wrapper](https://maven.apache.org/wrapper/) (`./mvnw`)
   handles the build tool.
3. Run the tests before and after your changes:
   ```bash
   ./mvnw verify
   ```

## Reporting Issues

- **Search first** to avoid duplicates.
- Provide a clear title and a concise description.
- Include steps to reproduce, expected vs. actual behavior, and your
  environment (OS, Java version).
- For crash reports, attach the relevant log/stack trace.

## Submitting Changes

1. Keep changes **small and focused** - one logical change per PR.
2. Follow the existing code style (Java 25, records, no unnecessary comments).
3. **Add or update tests** for any behavior change. All tests must pass.
4. Write a clear commit/PR message describing *what* and *why*.
5. Ensure the **CI build passes** (GitHub Actions runs `./mvnw verify`).
6. Reference related issues in the PR description when applicable.

## Code Style

- Java 25 features (records, sealed classes, pattern matching) are encouraged.
- Prefer immutable, type-safe models.
- Core logic belongs in `burrow-core`; keep the GUI layer in `burrow-gui`.
- No hardcoded credentials or keys.
- Run `./mvnw verify` to confirm formatting and tests.

## Use of AI Assistants

AI coding assistants may be used to support development. To keep the project
trustworthy and maintainable, contributors must follow these rules:

- **Understand the code you submit.** Any AI-generated (or assisted) code must
  be reviewed, understood, and verified by you before submission. Never submit
  code you cannot explain.
- **Test before submitting.** AI-generated changes must satisfy the same
  requirements as any other change, including passing `./mvnw verify`.
- **Attribute transparently.** If AI assistance was used substantially for a
  contribution, mention it in the PR description or commit message (e.g.
  "Co-authored with AI assistance"). Simple/non-substantial assistance does
  not require attribution.
- **No unsafe code.** Review generated code for security issues (credentials,
  secrets, unsafe deserialization) - never commit secrets or keys.

## Code of Conduct

Be respectful and constructive. Harassment and discrimination are not
tolerated. Focus on technical feedback, not on people.

## License

By contributing, you agree that your contributions are licensed under the
[Apache License, Version 2.0](LICENSE).
