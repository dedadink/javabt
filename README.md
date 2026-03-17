# javabt

Java FX backtesting/trading strategy runner using Dukascopy JForex API.

## Runtime prerequisites
- Java 21
- Maven 3.9+

## Build (Maven)
```bash
mvn -U clean compile
```

## Run (Maven classpath)
```bash
mvn -q exec:java -Dexec.mainClass=com.bot.fx.BacktestMain
```

## Environment variables
- No required environment variables for the current V1 backtest-only skeleton path.
- Legacy live/demo credentials (`DUKASCOPY_USER`, `DUKASCOPY_PASSWORD`) are intentionally not used in V1 runtime.

## Eclipse import
1. Open Eclipse.
2. `File` -> `Import` -> `Maven` -> `Existing Maven Projects`.
3. Select the repo root (`javabt`) and finish import.
4. Ensure Installed JRE / Execution Environment is Java 21.
5. If dependencies do not resolve, right click project -> `Maven` -> `Update Project...`.

## Notes
- The project now resolves Dukascopy API from Maven (`com.dukascopy.api:JForex-API`) via Dukascopy's public repository.
- The local `JForex-3-SDK-3.6.34.jar` is not used by Maven and should not be required for compile.


## Entry points
- `com.bot.fx.BacktestMain` is the primary runnable entry point for V1.
- `com.bot.fx.Main` is a compatibility delegator to `BacktestMain`.
- `com.bot.fx.FXbot` remains the strategy implementation class to be used by the future tester runner.


## V1 runtime scope
- V1 is backtest-only.
- `com.bot.fx.BacktestMain` is the primary runnable entry point.
- `com.bot.fx.Main` is retained only as a compatibility delegator to `BacktestMain`.