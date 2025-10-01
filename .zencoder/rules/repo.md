---
description: Repository Information Overview
alwaysApply: true
---

# WCFC-Groups Information

## Summary
A Java application to help determine who needs to be added or removed from a Groups.io mailer
for the Wings of Carolina Flying Club. The project consists of a Java backend with a Svelte frontend.

## Structure
- **src/**: Java backend source code
- **client/**: Svelte frontend application
- **integration-tests/**: Integration test scripts and data
- **data/**: Application data storage
- **bruno/**: API testing collections
- **.mvn/**: Maven configuration files

## Language & Runtime
**Backend Language**: Java
**Version**: Java 21 (Azul Zulu OpenJDK)
**Build System**: Maven
**Package Manager**: Maven

**Frontend Language**: JavaScript (Svelte)
**Version**: Svelte 5
**Build System**: Vite
**Package Manager**: npm

## Dependencies
**Backend Dependencies**:
- undertow-core 2.3.19.Final (Web server)
- jackson-databind 2.20.0 (JSON processing)
- retrofit 3.0.0 (HTTP client)
- opencsv 5.12.0 (CSV processing)
- jsoup 1.21.2 (HTML parsing)
- morphia-core 2.5.0 (MongoDB ODM)
- mongodb-driver-sync 5.6.0 (MongoDB driver)
- logback 1.5.18 (Logging)

**Frontend Dependencies**:
- @sveltejs/kit 2.43.5
- @sveltejs/adapter-static 3.0.8
- svelte-pdf 1.0.27
- @beyonk/svelte-notifications 4.3.0

## Build & Installation
```bash
# Build the application
make

# Build Docker image
make build

# Run integration tests
make integration-tests
```

## Output Directories
Note that these directories are in .gitignore, so you will normally be denied access to them.
- **target/**: Compiled Java classes and output from the Maven build process
- **docker/**: Dockerfile and build files, mostly copied from src/main/resources

### Docker
**Base Image**: azul/zulu-openjdk-alpine:21-latest
**Configuration**: Uses Alpine Linux with Azul Zulu OpenJDK 21
**Dockerfile**: src/main/resources/Dockerfile

### Testing
**Unit Tests**: As of now there are no unit tests for this app.
**Integration Tests**: There are end-to-end integration tests in `integration-tests/`.  These use Playwright for browser automation and WireMock for mocking external APIs.
**Run Command**:
```bash
make integration-tests
```

## Main Entry Points
**Backend**: org.wingsofcarolina.groups.Groups (main class)
**Frontend**: client/src/routes/+page.svelte (main page)
