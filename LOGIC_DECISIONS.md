# LOGIC_DECISIONS.md

## Purpose

This file holds strategy and reasoning decisions defined by the project owner.

The builder/agent must not silently invent these rules.

If a logic choice is unresolved:
- leave it blank
- mark as TODO
- keep architecture flexible
- do not guess

---

## V1 Strategy Family

V1 uses one fixed strategy family and searches only numeric parameter combinations around it.

### V1 Strategy Family Summary
- support/resistance calculated from historical bar structure
- breakout detected when price moves outside calculated support/resistance
- if breakout fails and price returns inside range, setup resets
- after breakout, wait for minimum delay before retest can qualify
- valid retest must occur on breakout side of the broken level, not back inside the old range
- once price reaches valid retest zone, it must remain there for dwell time
- once dwell completes, execute retest trade
- trade management is fixed and narrow
- parameter search focuses on numeric values only

---

## 1. Supported Symbols

V1 symbols:
- GBPJPY
- XAUUSD
- AUDUSD

Per-symbol pip conventions for V1:
- GBPJPY = 0.01
- AUDUSD = 0.0001
- XAUUSD = 0.10

Important:
- pip handling must be explicitly configured per symbol
- do not infer XAUUSD pip size from generic broker point logic
- future overrides may be needed for management triggers if symbol behavior differs

---

## 2. Level Timeframe

Initial timeframe candidates:
- 4H
- D1

Decision:
- treat level timeframe as a configurable/testable parameter for V1

Initial timeframe weights:
- D1 = 1.5
- H4 = 1.0
- H1 = 0.5 if introduced later

---

## 3. Execution Timeframe

Execution logic is driven by retest timing and bar/tick progression around the retest.

Decision:
- use execution timing rules based on retest dwell logic
- keep execution logic narrow in V1
- no large candle-pattern family engine in V1

Important separation:
- levels are built from higher-timeframe historical bars
- breakout/retest lifecycle is evaluated separately
- tick-aware progression is separate from level calculation

---

## 4. Support / Resistance Definition

Support/resistance is calculated from historical OHLC highs/lows using clustered price zones.

Method:
- use enabled higher timeframes such as 4H and D1
- collect historical high and low points
- separate upper candidates above current price from lower candidates below current price
- ignore points too close to current price and too far away using configurable min/max pip distance
- cluster nearby price points into zones using pip-tolerance distance
- rank zones by hit frequency / weighted hits
- nearest strong valid upper zone becomes resistance
- nearest strong valid lower zone becomes support

Important:
- levels are zones/bands, not only one exact line
- pip conversion must be symbol-aware
- do not use a generic invented indicator replacement
- use the supplied clustered level logic / code approach

````
package com.chad;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * V1 clustered support/resistance calculator.
 *
 * Design goals:
 * - Works on plain bar data, so it is easy to test and reuse.
 * - Uses symbol-aware pip size supplied from SymbolSpec.
 * - Finds clustered high/low meeting zones above and below current price.
 * - Avoids hardcoded JPY / gold assumptions.
 * - Avoids heavy dependencies like DBSCAN for V1.
 *
 * Intended use:
 * - BacktestRunner fetches bars from Dukascopy/JForex history.
 * - BacktestRunner passes bars into this calculator.
 */
public class ClusteredLevelCalculator {

    public static final class SymbolSpec {
        private final String symbol;
        private final double pipSize;
        private final int digits;

        public SymbolSpec(String symbol, double pipSize, int digits) {
            this.symbol = Objects.requireNonNull(symbol);
            this.pipSize = pipSize;
            this.digits = digits;
        }

        public String getSymbol() {
            return symbol;
        }

        public double getPipSize() {
            return pipSize;
        }

        public int getDigits() {
            return digits;
        }
    }

    public static final class SimpleBar {
        private final long timeMillis;
        private final double high;
        private final double low;
        private final String timeframeName;
        private final double weight;

        public SimpleBar(long timeMillis, double high, double low, String timeframeName, double weight) {
            this.timeMillis = timeMillis;
            this.high = high;
            this.low = low;
            this.timeframeName = timeframeName;
            this.weight = weight;
        }

        public long getTimeMillis() {
            return timeMillis;
        }

        public double getHigh() {
            return high;
        }

        public double getLow() {
            return low;
        }

        public String getTimeframeName() {
            return timeframeName;
        }

        public double getWeight() {
            return weight;
        }
    }

    public static final class PricePoint {
        private final double price;
        private final String timeframeName;
        private final double weight;
        private final boolean highPoint;

        public PricePoint(double price, String timeframeName, double weight, boolean highPoint) {
            this.price = price;
            this.timeframeName = timeframeName;
            this.weight = weight;
            this.highPoint = highPoint;
        }

        public double getPrice() {
            return price;
        }

        public String getTimeframeName() {
            return timeframeName;
        }

        public double getWeight() {
            return weight;
        }

        public boolean isHighPoint() {
            return highPoint;
        }
    }

    public static final class LevelZone {
        private final double minPrice;
        private final double maxPrice;
        private final double averagePrice;
        private final double weightedHits;
        private final int rawHits;
        private final List<String> contributingTimeframes;

        public LevelZone(
                double minPrice,
                double maxPrice,
                double averagePrice,
                double weightedHits,
                int rawHits,
                List<String> contributingTimeframes
        ) {
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.averagePrice = averagePrice;
            this.weightedHits = weightedHits;
            this.rawHits = rawHits;
            this.contributingTimeframes = contributingTimeframes;
        }

        public double getMinPrice() {
            return minPrice;
        }

        public double getMaxPrice() {
            return maxPrice;
        }

        public double getAveragePrice() {
            return averagePrice;
        }

        public double getWeightedHits() {
            return weightedHits;
        }

        public int getRawHits() {
            return rawHits;
        }

        public List<String> getContributingTimeframes() {
            return contributingTimeframes;
        }

        @Override
        public String toString() {
            return "LevelZone{" +
                    "minPrice=" + minPrice +
                    ", maxPrice=" + maxPrice +
                    ", averagePrice=" + averagePrice +
                    ", weightedHits=" + weightedHits +
                    ", rawHits=" + rawHits +
                    ", contributingTimeframes=" + contributingTimeframes +
                    '}';
        }
    }

    public static final class Result {
        private final List<LevelZone> upperZones;
        private final List<LevelZone> lowerZones;

        public Result(List<LevelZone> upperZones, List<LevelZone> lowerZones) {
            this.upperZones = upperZones;
            this.lowerZones = lowerZones;
        }

        public List<LevelZone> getUpperZones() {
            return upperZones;
        }

        public List<LevelZone> getLowerZones() {
            return lowerZones;
        }
    }

    public static final class Config {
        private final double currentPrice;
        private final int pipTolerance;
        private final int minPipDistance;
        private final int maxPipDistance;
        private final int minClusterPoints;
        private final int maxZonesPerSide;

        public Config(
                double currentPrice,
                int pipTolerance,
                int minPipDistance,
                int maxPipDistance,
                int minClusterPoints,
                int maxZonesPerSide
        ) {
            this.currentPrice = currentPrice;
            this.pipTolerance = pipTolerance;
            this.minPipDistance = minPipDistance;
            this.maxPipDistance = maxPipDistance;
            this.minClusterPoints = minClusterPoints;
            this.maxZonesPerSide = maxZonesPerSide;
        }

        public double getCurrentPrice() {
            return currentPrice;
        }

        public int getPipTolerance() {
            return pipTolerance;
        }

        public int getMinPipDistance() {
            return minPipDistance;
        }

        public int getMaxPipDistance() {
            return maxPipDistance;
        }

        public int getMinClusterPoints() {
            return minClusterPoints;
        }

        public int getMaxZonesPerSide() {
            return maxZonesPerSide;
        }
    }

    public Result calculate(SymbolSpec symbolSpec, Config config, List<SimpleBar> bars) {
        if (bars == null || bars.isEmpty()) {
            return new Result(List.of(), List.of());
        }

        double pipSize = symbolSpec.getPipSize();

        double upperMin = config.getCurrentPrice() + config.getMinPipDistance() * pipSize;
        double upperMax = config.getCurrentPrice() + config.getMaxPipDistance() * pipSize;

        double lowerMin = config.getCurrentPrice() - config.getMaxPipDistance() * pipSize;
        double lowerMax = config.getCurrentPrice() - config.getMinPipDistance() * pipSize;

        List<PricePoint> upperPoints = new ArrayList<>();
        List<PricePoint> lowerPoints = new ArrayList<>();

        for (SimpleBar bar : bars) {
            double high = bar.getHigh();
            double low = bar.getLow();

            if (high >= upperMin && high <= upperMax) {
                upperPoints.add(new PricePoint(high, bar.getTimeframeName(), bar.getWeight(), true));
            }

            if (low >= lowerMin && low <= lowerMax) {
                lowerPoints.add(new PricePoint(low, bar.getTimeframeName(), bar.getWeight(), false));
            }
        }

        List<LevelZone> upperZones = clusterPoints(
                upperPoints,
                pipSize,
                config.getPipTolerance(),
                config.getMinClusterPoints(),
                config.getMaxZonesPerSide()
        );

        List<LevelZone> lowerZones = clusterPoints(
                lowerPoints,
                pipSize,
                config.getPipTolerance(),
                config.getMinClusterPoints(),
                config.getMaxZonesPerSide()
        );

        return new Result(upperZones, lowerZones);
    }

    private List<LevelZone> clusterPoints(
            List<PricePoint> points,
            double pipSize,
            int pipTolerance,
            int minClusterPoints,
            int maxZonesPerSide
    ) {
        if (points.isEmpty()) {
            return List.of();
        }

        double maxGap = pipTolerance * pipSize;

        List<PricePoint> sorted = new ArrayList<>(points);
        sorted.sort(Comparator.comparingDouble(PricePoint::getPrice));

        List<List<PricePoint>> rawClusters = new ArrayList<>();
        List<PricePoint> currentCluster = new ArrayList<>();
        currentCluster.add(sorted.get(0));

        for (int i = 1; i < sorted.size(); i++) {
            PricePoint prev = sorted.get(i - 1);
            PricePoint next = sorted.get(i);

            if (Math.abs(next.getPrice() - prev.getPrice()) <= maxGap) {
                currentCluster.add(next);
            } else {
                rawClusters.add(currentCluster);
                currentCluster = new ArrayList<>();
                currentCluster.add(next);
            }
        }
        rawClusters.add(currentCluster);

        List<LevelZone> zones = new ArrayList<>();
        for (List<PricePoint> cluster : rawClusters) {
            if (cluster.size() < minClusterPoints) {
                continue;
            }

            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            double sum = 0.0;
            double weightedSum = 0.0;
            double weightedHits = 0.0;

            Map<String, Integer> timeframeCounts = new HashMap<>();

            for (PricePoint point : cluster) {
                double price = point.getPrice();
                min = Math.min(min, price);
                max = Math.max(max, price);
                sum += price;
                weightedSum += price * point.getWeight();
                weightedHits += point.getWeight();
                timeframeCounts.merge(point.getTimeframeName(), 1, Integer::sum);
            }

            double averagePrice = weightedHits > 0.0
                    ? weightedSum / weightedHits
                    : sum / cluster.size();

            List<String> contributingTimeframes = timeframeCounts.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .map(Map.Entry::getKey)
                    .toList();

            zones.add(new LevelZone(
                    min,
                    max,
                    averagePrice,
                    weightedHits,
                    cluster.size(),
                    contributingTimeframes
            ));
        }

        zones.sort((a, b) -> {
            int weightedCompare = Double.compare(b.getWeightedHits(), a.getWeightedHits());
            if (weightedCompare != 0) {
                return weightedCompare;
            }
            return Integer.compare(b.getRawHits(), a.getRawHits());
        });

        if (zones.size() > maxZonesPerSide) {
            return new ArrayList<>(zones.subList(0, maxZonesPerSide));
        }

        return zones;
    }
}

````
---

## 5. Level Merge / Distance Rule

Decision:
- use clustered band logic
- exact numeric merge rules remain configurable if needed

---

## 6. Breakout Definition

Definition:
- breakout occurs when price moves outside a calculated support or resistance level
- after breakout, wait for configured timing rules
- if price returns back inside the old range before retest qualifies, mark as back-in-range and reset

Decision:
- keep breakout logic narrow and deterministic
- breakout buffer may remain a configurable numeric variable if added

---

## 7. Retest Minimum Delay

Definition:
- minimum time after breakout before retest is allowed to qualify

Initial V1 search values:
- 15m
- 30m
- 45m

---

## 8. Retest Maximum Window

Definition:
- maximum time after breakout during which a valid retest may still occur

Initial V1 search values:
- 3 days

---

## 9. Retest Tolerance

Definition:
- how close to the broken level price must return to count as a valid retest-zone interaction

Initial V1 search values:
- 2 pips
- 5 pips
- 10 pips

Important rule:
Retest tolerance must remain on the breakout side of the broken level.

- bullish breakout above resistance:
  - valid retest zone is above broken resistance only
  - back inside the old range is invalid / back-in-range
- bearish breakout below support:
  - valid retest zone is below broken support only
  - back inside the old range is invalid / back-in-range

Worked examples:

### Example 1 — bullish retest invalid
- breakout above 170.650
- valid retest zone = 170.650 to 170.700
- if price drops to 170.640, that is back inside the old range
- this is invalid for bullish retest
- mark BACK_IN_RANGE and reset

### Example 2 — bullish retest valid
- breakout above 170.650
- valid retest zone = 170.650 to 170.700
- if price drops to 170.675 and this is within tolerance, it is a valid bullish retest candidate

### Example 3 — bearish retest invalid
- breakout below 170.650
- valid retest zone = 170.650 to 170.600
- if price rises to 170.660, that is back inside the old range
- this is invalid for bearish retest
- mark BACK_IN_RANGE and reset

### Example 4 — bearish retest valid
- breakout below 170.650
- valid retest zone = 170.650 to 170.600
- if price rises to 170.625 and this is within tolerance, it is a valid bearish retest candidate

---

## 10. Retest Dwell Time

Definition:
- once price re-enters the valid retest tolerance zone, how long it must stay there before entry is allowed

Initial V1 search values:
- 15m
- 30m
- 45m

---

## 11. Retest Invalid / Inconclusive Rule

Decision:
- if price returns back inside the old range, mark as back-in-range and reset
- if retest does not qualify within retestMaximumWindow, expire setup
- more advanced invalidation rules are deferred

---

## 12. Recalculation Timing

Decision:
- after invalid or expired setup, return to normal level-calculation cycle
- after executed trade, cooldown applies until next market day

---

## 13. Confirmation Rule

Decision:
- confirmation is based on price staying within the valid retest tolerance zone for the configured dwell time
- no advanced candle-pattern family in V1

---

## 14. Entry Timing

Decision:
- execute immediately after dwell/confirmation completes

---

## 15. Stop Loss Logic

Corrected rule:
- bullish retest trade after resistance breakout:
  - SL = broken resistance minus stop buffer
- bearish retest trade after support breakout:
  - SL = broken support plus stop buffer

Exact stop buffer values:
- TODO / configurable

---

## 16. Take Profit Logic

Decision:
- run a separate target-level calculation using the broken support/resistance context as the new reference point
- do not replace the current active level set for setup detection
- TP is the newly identified next target level in the direction of trade

Examples:
- bullish retest trade:
  - breakout above 170.650
  - retest valid above broken resistance
  - SL = 170.650 minus stop buffer
  - separate target calculation is run using broken reference context 170.650
  - TP = next valid resistance above
  - allow execution only if RR >= minimumRR

- bearish retest trade:
  - breakout below 170.650
  - retest valid below broken support
  - SL = 170.650 plus stop buffer
  - separate target calculation is run using broken reference context 170.650
  - TP = next valid support below
  - allow execution only if RR >= minimumRR

---

## 17. Minimum Risk/Reward Rule

Definition:
- RR = reward / risk
- trade is only allowed if RR >= minimumRR

Decision:
- minimumRR should be configurable
- initial value or search range:
  - TODO

---

## 18. Partial Close Logic

Decision:
- at 50 percent of path to TP:
  - close 50 percent of position
  - move SL to breakeven
- at 50 percent of path to SL:
  - close 50 percent of position
- each partial event is one-time only

Mirror the successful one-time-event behavior from the MQL implementation.

---

## 19. Breakeven Logic

Decision:
- move SL to breakeven after TP-side 50 percent partial close event

---

## 20. Cooldown / Re-entry Logic

Decision:
- wait until next market day

---

## 21. Session Filter

Decision:
- no trading 1 hour before market close
- no trading 2 hours after market open

---

## 22. Spread / Slippage Assumptions

Current decision:
- trading-time exclusion around market close/open is required
- exact spread/slippage modeling values still TODO

---

## 23. Position Sizing

Decision:
- TODO

---

## 24. Success Criteria for Candidate Strategy

Current decision:
- balance growth over most recent 1 month period is important

Expansion required:
- minimum trade count
- max drawdown cap
- optionally additional quality metrics later

A recent profitable month alone is not sufficient if sample size or drawdown quality is poor.

---

## 25. Recent Performance Emphasis

Decision:
- recent performance should be shown clearly
- most recent 1 month period matters heavily

---

## 26. Candidate Review Policy

Decision:
- future live use requires manual review
- future live strategy may be shut down after market change or deterioration threshold
- live phase is deferred from V1

---

## Deferred / V2+ Decisions

These are intentionally not part of V1 logic search.

### Deferred Filters / Logic
- HH/HL / LH/LL trend scoring
- bullish/bearish regime weighting
- multi-timeframe market structure bias
- multiple breakout families
- multiple retest families
- multiple candle confirmation families

### Deferred Live / Operational Decisions
- demo/live runner behavior
- auto-promotion rules
- live shutdown and replacement flow
- portfolio/correlation logic