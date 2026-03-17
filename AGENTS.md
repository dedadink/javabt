# AGENTS.md

## Project Mission

Build V1 historical strategy-finder backtester only for Dukascopy / JForex.

V1 must:
- calculate support and resistance levels from historical bar data
- detect breakout and retest setups
- simulate trade execution and trade management
- sweep numeric parameter combinations
- store and rank results
- export promising candidate strategies for manual review

V1 must not implement live or demo trading yet.

The codebase should be structured so a future `LiveMain` can reuse the same strategy logic later, but V1 itself is backtest-only.

---

## Scope Boundaries

### V1 Includes
- historical backtest runner
- parameter grid execution
- support/resistance calculation
- breakout/retest trade logic
- tick-aware or intrabar-aware trade progression
- trade management simulation
- result persistence
- candidate ranking/export
- multi-symbol compatibility in backtest mode

### V1 Excludes
- live trading
- demo trading
- auto-deployment of winning strategies
- account-connected execution loops
- advanced market regime scoring
- HH/HL / LH/LL trend filters
- multiple competing logic families
- UI/dashboard unless needed for debug only

---

## Core Build Principle

V1 searches numeric parameter combinations inside one fixed strategy family.

Do not broaden V1 into multiple competing logic families.

Examples:
- OK: retest minimum delay = 15m / 30m / 45m
- OK: retest tolerance = 2 / 5 / 10 pips
- OK: retest dwell time = 15m / 30m / 45m
- OK: level timeframe = 4H / D1 if explicitly configured as a parameter
- OK: minimum RR = a small fixed value or a small numeric range

- NOT OK for V1: many breakout logic models
- NOT OK for V1: many candle-pattern families
- NOT OK for V1: trend regime scoring variants
- NOT OK for V1: automatic live promotion

---

## Non-Negotiable Architecture Rules

- Keep one repo
- Build backtest-only V1
- Use shared strategy logic modules
- Separate:
  - strategy logic
  - execution/backtest runner
  - parameter generation
  - result persistence
  - ranking / candidate export
- Future architecture may support:
  - BacktestMain
  - LiveMain
- But only BacktestMain is in scope for V1

Do not mix historical backtest orchestration with realtime account execution in the same runner.

---

## Code Guard Rails

- Do not silently invent trading rules when requirements are unclear
- Use LOGIC_DECISIONS.md as source of truth for strategy behavior
- If logic is unresolved, keep placeholders explicit rather than guessing
- Prefer deterministic, testable modules over large monolithic classes
- Avoid hiding all logic inside one giant strategy class
- Keep each backtest run reproducible from:
  - strategy config
  - research window
  - code version
  - symbol
  - parameter values
- Do not rank results on profit alone
- Do not auto-promote any strategy to live use
- Save all tested results, not just the winner

---

## Data / Simulation Guard Rails

- Avoid lookahead bias
- Levels must be calculated only from information available at that historical point
- Distinguish clearly between:
  - level-calculation timeframe
  - execution timeframe
  - intrabar / tick progression
- Do not describe bar data as tick data
- Do not assume OHLC bars reveal true tick path unless explicitly modeled
- Where possible, use historical tick data for trigger / management realism
- Where approximation is used, document it clearly in code and outputs

Important separation:
- levels are built from higher-timeframe historical bars
- breakout/retest lifecycle is evaluated separately
- tick-aware progression is separate from level calculation

That separation is important.

---

## Strategy Search Philosophy

Goal is not to find one magical isolated winning combo.

Goal is to find:
- a promising parameter region
- for one clearly defined strategy family
- that survives basic validation
- and still performs well in recent periods

A stronger result is a stable cluster of nearby settings, not one random standout.

Recent performance matters heavily.

---

## Validation Guard Rails

Promising strategy candidates should later be judged with:
- recent-performance emphasis
- minimum trade count
- drawdown ceiling
- minimum reward/risk threshold at trade level
- optional profit factor / expectancy filters later

Recent performance matters more than distant historical success.

---

## Multi-Symbol Guard Rail

The system is expected to support multiple symbols in backtest mode.

Critical risk:
- pip size / pip value / price precision handling differs by symbol
- incorrect pip conversion can silently break:
  - tolerances
  - breakout buffers
  - level distances
  - SL / TP
  - partial management thresholds
  - RR logic

Therefore:
- pip math must be centralized
- symbol metadata must be explicit
- no global hardcoded pip assumptions
- each symbol must use correct precision / pip rules
- one symbol's pip conventions must not break another symbol's logic

Initial V1 pip conventions:
- GBPJPY = 0.01
- AUDUSD = 0.0001
- XAUUSD = 0.10

Do not infer XAUUSD pip size from generic broker point logic.

Architecture should allow future symbol-specific overrides for management-distance behavior.

---

## Level Calculation Guard Rail

Do not invent a generic support/resistance algorithm.

Use the project-defined clustered OHLC level logic from LOGIC_DECISIONS.md or supplied code.

Preferred V1 implementation is a clustered historical OHLC zone calculator using:
- symbol-aware pip conversion
- separate upper/lower clustering
- timeframe weights
- plain bar objects as inputs
- no direct Dukascopy API coupling inside the calculator

If existing level-calculation code or algorithm is supplied, prefer adapting that logic instead of replacing it with a different invented method.

---

## MQL Porting Guidance

When porting logic from the user's MQL implementation, preserve these patterns where appropriate:
- explicit per-symbol pip overrides
- explicit TP trigger overrides where needed
- one-time trade state flags
- no repeated partial closes
- no repeated breakeven modification once already applied

Good MQL patterns to mirror:
- `PipSize(symbol)` with explicit overrides
- `TpPartialPipsFor(symbol)` style override hooks
- per-trade state flags equivalent to:
  - `slClosed`
  - `tpClosed`
  - `tpAdjusted`

Do not copy MT5 API mechanics directly, but preserve the behavior design.

---

## Handling Unclear Requirements

If a build step depends on unresolved trading logic:
1. do not guess silently
2. add a TODO or placeholder
3. record the gap in LOGIC_DECISIONS.md
4. keep architecture moving without inventing strategy behavior

---

## Deferred / Not for V1

Do not implement these in V1 unless explicitly re-scoped:
- market regime scoring
- bearish/bullish weighting models
- HH/HL / LH/LL structure filters
- multi-timeframe trend scoring
- many candle confirmation families
- many breakout logic families
- many retest logic families
- live/demo trading runner
- auto-promotion to live
- portfolio/correlation controls
- advanced walk-forward framework