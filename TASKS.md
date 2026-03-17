# TASKS.md

## V1 Goal

Build a historical strategy-finder backtesting system that:

- computes support and resistance levels
- tests breakout and retest logic
- simulates defined trade-management behavior
- sweeps numeric parameter ranges
- records all results
- ranks candidates
- exports promising strategy configurations for manual review

V1 is backtest only.

No live or demo execution in V1.

---

## V1 Search Philosophy

V1 searches numeric parameter combinations inside one fixed strategy family.

V1 does not search across multiple competing logic families.

Primary V1 numeric search dimensions may include:
- level timeframe
- retest minimum delay
- retest dwell time
- retest tolerance
- breakout buffer
- stop buffer
- minimum RR
- other approved numeric variables

---

## Phase 1 — Create the V1 architecture skeleton

### Objective
Refactor toward a clean backtest architecture.

### Deliverables
Create the package/class skeleton for:

- BacktestMain
- BacktestRunner
- StrategyConfig
- ParameterGrid
- ResearchWindow
- SymbolSpec
- LevelCalculator
- BreakoutDetector
- RetestDetector
- EntryPlanner
- TradeManager
- TradeSimulator
- BacktestResult
- TradeRecord
- CandidateStrategy
- ResultRepository
- ResultRanker

### Acceptance Criteria
- project compiles
- V1 entry point is backtest-oriented
- architecture is clearly separated by responsibility
- future LiveMain is possible later, but not implemented now

---

## Phase 2 — Replace mixed settings with clean config objects

### Objective
Split one concrete backtest run from the parameter ranges that generate it.

### Required Classes

#### StrategyConfig
Represents one exact backtest run:
- symbol
- symbol precision metadata
- pip size / pip conversion info
- level timeframe
- execution timeframe
- level search rules
- level merge / min distance rule
- breakout buffer
- retest minimum delay
- retest maximum window
- retest tolerance
- retest dwell time
- confirmation rule
- stop buffer
- SL settings
- TP settings
- minimum RR
- trade management settings
- cooldown settings
- session / exclusion window settings
- spread/slippage placeholders
- research window reference

#### ParameterGrid
Represents all numeric ranges to test:
- level timeframe values if variable
- breakout buffer values
- retest minimum delay values
- retest maximum window values
- retest tolerance values
- retest dwell time values
- stop buffer values
- minimum RR values
- other numeric variables approved for V1

#### ResearchWindow
Represents explicit historical window:
- start datetime
- end datetime
- optional recent-performance inspection window
- optional validation split

#### SymbolSpec
Represents per-symbol rules:
- instrument name
- price precision
- pip size
- formatting rules
- optional override hooks for management-distance behavior
- any required symbol-specific execution metadata

### Acceptance Criteria
- one StrategyConfig = one reproducible test run
- no execution code depends directly on raw range arrays
- symbol-specific pip handling is explicit and centralized

---

## Phase 3 — Implement support/resistance engine V1

### Objective
Implement the project-defined level-calculation method.

### Required Behavior
- use historical bar data for level calculation
- use 4H and/or D1 data as configured
- inspect historical OHLC highs/lows over long periods
- find common clustered touch zones above and below current price
- identify nearest valid resistance zone above current price
- identify nearest valid support zone below current price
- represent levels as a band/zone, not only one exact number
- support timeframe weights

Initial V1 timeframe weights:
- D1 = 1.5
- H4 = 1.0
- H1 = 0.5 if introduced later

Implementation note:
- prefer the supplied clustered level-calculator approach
- data loading from Dukascopy should be separated from the clustering logic
- level calculator should operate on plain bar objects, not directly on Dukascopy API calls

Important separation:
- levels are built from higher-timeframe historical bars
- breakout/retest lifecycle is evaluated separately
- tick-aware progression is separate from level calculation

### Acceptance Criteria
- levels come from historical clustered price structure
- levels are deterministic for the same config/data
- no future data is used at the decision point

---

## Phase 4 — Implement breakout state detection

### Objective
Detect when price meaningfully breaks a calculated level.

### Required Behavior
- detect breakout above resistance / below support
- breakout occurs when price moves outside the calculated level using configured breakout rules
- if breakout later fails and price returns back inside range before qualifying, mark as back-in-range and reset
- use correct pip math per symbol
- mark breakout event with time, level reference, direction
- prevent duplicate breakout spam from the same setup

### Acceptance Criteria
- breakout detection is independent from trade placement
- breakout events can be stored and handed to retest logic
- symbol pip conversion does not distort breakout thresholds

---

## Phase 5 — Implement retest-watch state machine

### Objective
Track post-breakout behavior and decide whether a retest becomes valid.

### Required Behavior
For each breakout:
- start a retest-watch state
- do not allow retest qualification before retestMinimumDelay
- allow retest checking until retestMaximumWindow
- use tick-aware or intrabar-aware progression where possible
- a bullish retest zone must remain on the breakout side of resistance, not back inside the old range
- a bearish retest zone must remain on the breakout side of support, not back inside the old range
- once price enters the valid retest tolerance zone, begin retest dwell tracking
- if price remains in the valid retest zone for retestDwellTime, mark retest confirmed
- if price leaves the valid retest zone before dwell completes, reset or invalidate according to logic decisions
- invalidate setup if timeout expires
- invalidate setup if defined failure condition occurs
- after invalidation, return control to recalculation / next cycle logic

### Core State Ideas
- BREAKOUT_DETECTED
- WAITING_FOR_MIN_DELAY
- WATCHING_FOR_RETEST
- RETEST_IN_ZONE
- RETEST_CONFIRMED
- RETEST_INVALIDATED
- RETEST_EXPIRED
- BACK_IN_RANGE

### Acceptance Criteria
- retest minimum delay is a configurable numeric parameter
- retest dwell time is a separate configurable numeric parameter
- retest maximum window is a configurable numeric parameter
- expired or invalidated retests do not linger forever

---

## Phase 6 — Implement V1 confirmation / entry planning

### Objective
Convert a valid retest into a concrete trade candidate.

### Required Behavior
- create trade candidate from confirmed retest
- entry timing is immediate once configured dwell/confirmation completes
- for bullish retest: entry on buy side near broken resistance from above
- for bearish retest: entry on sell side near broken support from below
- define SL and TP
- reject trade if minimum RR threshold is not met
- enforce one-trade-per-setup rule unless otherwise defined

### Worked Examples

#### Example A — bullish breakout, valid retest, back-in-range invalidation
- breakout above 170.650
- valid retest zone = 170.650 to 170.700
- if price drops to 170.640, that is back inside the old range
- this is not a valid bullish retest
- mark BACK_IN_RANGE and reset

#### Example B — bullish breakout, valid retest on breakout side
- breakout above 170.650
- valid retest zone = 170.650 to 170.700
- if price drops to 170.675 and remains within the valid retest zone for dwell time, this is a valid bullish retest

#### Example C — bearish breakout, valid retest, back-in-range invalidation
- breakout below 170.650
- valid retest zone = 170.650 to 170.600
- if price rises to 170.660, that is back inside the old range
- this is not a valid bearish retest
- mark BACK_IN_RANGE and reset

#### Example D — bullish trade execution and TP selection
- breakout above 170.650
- valid retest zone = 170.650 to 170.700
- if price drops to 170.675 and this is within execution tolerance and dwell/confirmation completes:
  - bullish entry is valid
  - SL = 170.650 minus stop buffer, for example 2 pips
  - run a separate target-level calculation using 170.650 as the broken reference context
  - if the newly calculated resistance target gives RR above minimumRR, allow execution

#### Example E — bearish trade execution and TP selection
- breakout below 170.650
- valid retest zone = 170.650 to 170.600
- if price rises to 170.625 and this is within execution tolerance and dwell/confirmation completes:
  - bearish entry is valid
  - SL = 170.650 plus stop buffer, for example 2 pips
  - run a separate target-level calculation using 170.650 as the broken reference context
  - if the newly calculated support target gives RR above minimumRR, allow execution

### Acceptance Criteria
- entry rules are deterministic
- V1 uses only one narrow confirmation model
- no multi-family candle-pattern engine is introduced

---

## Phase 7 — Implement trade simulation and management

### Objective
Model the trade lifecycle realistically enough for backtest use.

### Required Behavior
- open synthetic trade from approved candidate
- simulate price progression
- support long and short symmetry
- support SL / TP hit logic
- support partial close logic
- support breakeven move logic
- support final close reasons

### V1 Trade Logic Requirements
- bullish retest trade:
  - SL below broken resistance by stop buffer
  - TP at next recalculated target level above
- bearish retest trade:
  - SL above broken support by stop buffer
  - TP at next recalculated target level below
- at 50 percent of distance to TP:
  - close 50 percent of position
  - move SL to breakeven
- at 50 percent of distance to SL:
  - close 50 percent of position
- trade must be rejected if RR is below minimumRR

### Porting Guidance from MQL
Mirror the one-time event behavior from the working MQL logic:
- `slClosed`
- `tpClosed`
- `tpAdjusted`

These events must not repeat endlessly.

### Required Per-Trade State
- open
- slPartialTaken
- tpPartialTaken
- movedToBreakeven
- closed
- closeReason

### Acceptance Criteria
- each trade has a deterministic lifecycle
- each trade finishes in exactly one final state
- partial-close logic cannot repeat endlessly on the same trade

---

## Phase 8 — Implement intrabar / tick-awareness

### Objective
Keep backtests behaviorally close to live flow where practical.

### Required Behavior
- use bar data for level calculation and structure
- use tick or intrabar-aware progression for breakout, retest, and trade management where possible
- do not treat OHLC bars as equivalent to true tick path
- document fallback approximation clearly if full tick resolution is unavailable in a stage of logic

### Acceptance Criteria
- execution realism assumptions are visible in code and outputs
- bar-vs-tick distinction is explicit
- trade triggers and retest checks are not based on sloppy pip shortcuts

---

## Phase 9 — Implement result persistence

### Objective
Store all tested runs and all trade outcomes in structured form.

### Required Classes
- BacktestResult
- TradeRecord
- CandidateStrategy
- ResultRepository

### Minimum BacktestResult Fields
- config ID / config hash
- code version
- symbol
- research window
- recent-performance window
- parameter values
- trade count
- net profit
- gross profit
- gross loss
- drawdown
- win rate
- expectancy
- profit factor if implemented
- average RR
- invalidated setup counts
- expired retest counts
- back-in-range reset counts

### Minimum TradeRecord Fields
- trade ID
- config ID
- symbol
- setup reference
- level reference
- breakout time
- retest confirm time
- direction
- entry time / entry price
- SL / TP
- partial events
- breakeven move
- exit time / exit price
- close reason
- pips result
- currency result

### Acceptance Criteria
- every run can be reproduced later
- every run can be inspected later
- all combinations are saved, not just winners

---

## Phase 10 — Implement ranking and candidate export

### Objective
Find promising candidate regions, not just one lucky top row.

### Required Behavior
- rank strategies on multiple metrics, not profit alone
- enforce minimum trade count
- inspect recent-performance behavior
- preserve nearby parameter results for stability inspection
- export promising candidates for manual review

### Simple V1 Ranking Idea
Use a weighted score that rewards:
- recent net growth
- lower drawdown
- sufficient trade count
- acceptable RR quality

And penalizes:
- tiny sample size
- weak recent period
- oversized drawdown

### Candidate States
- NEW
- PROMISING
- REJECTED
- APPROVED placeholder only for future use

### Important Design Rule
A strong candidate is not only a single best result.
The system should help reveal whether nearby parameter combinations are also decent.

### Acceptance Criteria
- output supports parameter-region inspection
- exported results support manual review
- no auto-promotion to live mode

---

## Phase 11 — Add basic validation guard rails

### Objective
Reduce obvious overfitting.

### Required Behavior
- support minimum trade count
- support recent-performance emphasis
- support at least a simple recent-period summary
- include spread/slippage placeholders
- reject tiny-sample winners

### Acceptance Criteria
- result quality is not judged by profit alone
- recent performance can be inspected clearly
- isolated lucky runs are easier to identify

---

## Phase 12 — Multi-symbol pip and precision safety

### Objective
Prevent symbol-specific pip math from corrupting the strategy engine.

### Required Behavior
- centralize pip calculations
- use per-symbol metadata
- test pip conversion explicitly for GBPJPY, XAUUSD, and AUDUSD
- never let one symbol's assumptions leak into another's logic

Initial V1 pip conventions:
- GBPJPY -> 0.01
- AUDUSD -> 0.0001
- XAUUSD -> 0.10

Do not attempt to auto-infer these from generic point size rules.

Allow future override fields for symbols with unusual management-distance behavior.

### Acceptance Criteria
- symbol pip math is centralized and testable
- multi-symbol backtests do not reuse incorrect generic pip assumptions
- all tolerance and distance logic uses correct symbol-aware conversions

---

## Phase 13 — Isolate or retire legacy shell behavior

### Objective
Keep V1 clean.

### Required Behavior
- isolate or retire legacy realtime/demo runner assumptions
- isolate or retire placeholder level logic
- isolate or retire thin CSV-only result flow if it blocks structured results
- keep V1 path clean and backtest-only

### Acceptance Criteria
- V1 success does not depend on demo account execution
- legacy code no longer dictates architecture
- the backtest path is the primary supported workflow

---

## Deferred / V2+

These items are intentionally culled from V1.

### Deferred Logic Families
- multiple breakout logic families
- multiple retest logic families
- multiple candle confirmation families
- advanced inconclusive branching beyond timeout / back-in-range / invalidate flow

### Deferred Filters / Scoring
- market regime scoring
- bearish/bullish weighting
- HH/HL / LH/LL trend structure filters
- multi-timeframe trend bias engine

### Deferred Execution / Operations
- demo/live runner
- auto-promotion of candidates to live use
- live shutdown/replacement management
- account-linked execution safety systems

### Deferred Research Features
- advanced walk-forward optimization
- portfolio / correlation controls
- adaptive logic switching