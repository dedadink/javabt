# AGENTS.md

Project rules:

- Standardize on Java 21.
- Use Maven for dependency management.
- Do not use manual jar/classpath hacks unless Maven is impossible.
- Preserve trading logic during refactors.
- Prefer minimal changes that restore compile/run capability first.
- Report dependency/build issues separately from Java/API/source issues.

Build:

- Primary local build command:
  mvn -U clean compile

Do not revisit pom.xml or dependency setup unless a source-level issue proves it is necessary
