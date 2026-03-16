# AGENTS.md

Project rules:

- Java version: 21
- Build system: Maven
- Primary build command: mvn clean compile

Guidelines:

- Do not use manual jar/classpath hacks unless Maven is impossible.
- Prefer Maven dependencies for Dukascopy libraries.
- Preserve trading logic during refactors.
- Fix dependency issues before modifying strategy logic.

Build workflow:

1. mvn clean compile
2. resolve dependency issues
3. then address API breakages

disregard "JForex-3-SDK-3.6.34.jar" uploaded committed file, i cannot delete due to error.

We are standardizing this repo on Java 21 and Maven.\n\nTasks:\n1. Create or repair pom.xml so the project builds as a Maven project.\n2. Add the official Dukascopy public Maven repository.\n3. Add the correct JForex dependency that provides com.dukascopy.api.* classes.\n4. Set compiler source/target to Java 21.\n5. Remove any dependency on the broken local SDK jar unless absolutely required.\n6. Do not use manual jar/classpath hacks unless Maven is impossible.\n7. Run compile checks and report remaining API breakages separately from dependency issues.\n8. If multiple Dukascopy artifacts are possible, identify the right one and explain why.\n9. Keep code changes minimal until the project compiles.\n\nOutput:\n- final pom.xml\n- local build command\n- Eclipse import steps\n- any environment variables needed

Do not revisit pom.xml or dependency setup unless a source-level issue proves it is necessary
