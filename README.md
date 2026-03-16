diff --git a/README.md b/README.md
index e2c24b4b004941fed41250c67196eb17280a9731..338fc364d9fc97c2ae4621444d207a4d94e0a576 100644
--- a/README.md
+++ b/README.md
@@ -1,2 +1,34 @@
-java backtesting system, using dukascopy documentation for jforex.
-old version, happy to consider updating code with new v21 per documentation.
+# javabt
+
+Java FX backtesting/trading strategy runner using Dukascopy JForex API.
+
+## Runtime prerequisites
+- Java 21
+- Maven 3.9+
+
+## Build (Maven)
+```bash
+mvn -U clean compile
+```
+
+## Run (Maven classpath)
+```bash
+export DUKASCOPY_USER="your-demo-user"
+export DUKASCOPY_PASSWORD="your-demo-password"
+mvn -q exec:java -Dexec.mainClass=com.bot.fx.Main
+```
+
+## Environment variables
+- `DUKASCOPY_USER`
+- `DUKASCOPY_PASSWORD`
+
+## Eclipse import
+1. Open Eclipse.
+2. `File` -> `Import` -> `Maven` -> `Existing Maven Projects`.
+3. Select the repo root (`javabt`) and finish import.
+4. Ensure Installed JRE / Execution Environment is Java 21.
+5. If dependencies do not resolve, right click project -> `Maven` -> `Update Project...`.
+
+## Notes
+- The project now resolves Dukascopy API from Maven (`com.dukascopy.api:JForex-API`) via Dukascopy's public repository.
+- The local `JForex-3-SDK-3.6.34.jar` is not used by Maven and should not be required for compile.
