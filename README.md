# Jinjecte

**Jinjecte** is a lightweight command-line utility that allows you to attach to running Java Virtual Machines (JVMs) and perform runtime operations such as querying system properties or injecting Java agents.

## Features

- List all active JVM processes on your system.
- Attach to a selected JVM using the Attach API.
- Display basic JVM information: Java version, vendor, OS, architecture, and user.
- Query specific JVM system properties.
- Inject Java agents into any attached JVM.
- Interactive CLI with helpful commands: `help`, `info`, `property`, `inject`, `exit`.

## Downloads

You can download **Jinjecte** directly from my Maven repository: [https://maven.nozyx.dev/dev/nozyx/jinjecte](https://maven.nozyx.dev/dev/nozyx/jinjecte)

## Requirements

- **JDK 11** or higher with attach tools included.
- The tool works on any OS where the JDK supports the Attach API.

## Usage

1. Run Jinjecte using the JAR via `java -jar jinjecte-1.0.0.jar`.
2. Choose a JVM process from the list.
3. Use commands like:
    - `help` – Show all available commands.
    - `info` – Display basic JVM information.
    - `property <property-name>` – Retrieve a specific system property.
    - `inject <path-to-agent>` – Inject a Java agent into the attached JVM.
    - `exit` – Detach and return to the JVM selection menu.

## License

**NPL (v1)** – [See LICENSE file](./LICENSE)