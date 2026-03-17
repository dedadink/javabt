package com.chad;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;

public class InstrumentSettings {
    private Instrument instrument;
    private double pipValue;
    private double[] riskPercentRange;
    private double[] levelSearchRangeRange;
    private double[] minLevelDistanceRange;
    private double[] zoneRangeRange;
    private int[] historyMonthsRange;
    private double[] minPipsTPRange;
    private double[] minPipsSLRange;
    private double[] maxExposurePercentRange;
    private double[] toleranceRange;
    private int[] checkCounterRange;
    private Period levelCalculationPeriod;
    private Period tradeExecutionPeriod;
    private long startTime;
    private long endTime;
    private double maxLossPercent;
    private boolean waitForNextCandle;
    private long postTradeWaitTime;
    private double slPartialClosurePercentage;
    private double tpPartialClosurePercentage;
    private double currentLevelSearchRange;

    public InstrumentSettings(
            Instrument instrument, double pipValue, double[] riskPercentRange, double[] levelSearchRangeRange,
            double[] minLevelDistanceRange, double[] zoneRangeRange, int[] historyMonthsRange, double[] minPipsTPRange,
            double[] minPipsSLRange, double[] maxExposurePercentRange, double[] toleranceRange, int[] checkCounterRange,
            Period levelCalculationPeriod, Period tradeExecutionPeriod, long startTime, long endTime, double maxLossPercent,
            boolean waitForNextCandle, long postTradeWaitTime, double slPartialClosurePercentage,
            double tpPartialClosurePercentage) {
        this.instrument = instrument;
        this.pipValue = pipValue;
        this.riskPercentRange = riskPercentRange;
        this.levelSearchRangeRange = levelSearchRangeRange;
        this.minLevelDistanceRange = minLevelDistanceRange;
        this.zoneRangeRange = zoneRangeRange;
        this.historyMonthsRange = historyMonthsRange;
        this.minPipsTPRange = minPipsTPRange;
        this.minPipsSLRange = minPipsSLRange;
        this.maxExposurePercentRange = maxExposurePercentRange;
        this.toleranceRange = toleranceRange;
        this.checkCounterRange = checkCounterRange;
        this.levelCalculationPeriod = levelCalculationPeriod;
        this.tradeExecutionPeriod = tradeExecutionPeriod;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxLossPercent = maxLossPercent;
        this.waitForNextCandle = waitForNextCandle;
        this.postTradeWaitTime = postTradeWaitTime;
        this.slPartialClosurePercentage = slPartialClosurePercentage;
        this.tpPartialClosurePercentage = tpPartialClosurePercentage;
    }

    public void setCurrentLevelSearchRange(double levelSearchRange) {
        this.currentLevelSearchRange = levelSearchRange;
    }

    public double getSlPartialClosurePercentage() {
        return slPartialClosurePercentage;
    }

    public double getTpPartialClosurePercentage() {
        return tpPartialClosurePercentage;
    }

    public double getPipValue() {
        return pipValue;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public double[] getRiskPercentRange() {
        return riskPercentRange;
    }

    public double[] getLevelSearchRangeRange() {
        return levelSearchRangeRange;
    }

    public double[] getMinLevelDistanceRange() {
        return minLevelDistanceRange;
    }

    public double[] getZoneRangeRange() {
        return zoneRangeRange;
    }

    public int[] getHistoryMonthsRange() {
        return historyMonthsRange;
    }

    public double[] getMinPipsTPRange() {
        return minPipsTPRange;
    }

    public double[] getMinPipsSLRange() {
        return minPipsSLRange;
    }

    public double[] getMaxExposurePercentRange() {
        return maxExposurePercentRange;
    }

    public double[] getToleranceRange() {
        return toleranceRange;
    }

    public double getCurrentLevelSearchRange() {
        return currentLevelSearchRange;
    }

    public int[] getCheckCounterRange() {
        return checkCounterRange;
    }

    public Period getLevelCalculationPeriod() {
        return levelCalculationPeriod;
    }

    public Period getTradeExecutionPeriod() {
        return tradeExecutionPeriod;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public double getMaxLossPercent() {
        return maxLossPercent;
    }

    public boolean isWaitForNextCandle() {
        return waitForNextCandle;
    }

    public long getPostTradeWaitTime() {
        return postTradeWaitTime;
    }

    @Override
    public String toString() {
        return "InstrumentSettings{" +
                "instrument=" + instrument +
                ", pipValue=" + pipValue +
                ", riskPercent=" + java.util.Arrays.toString(riskPercentRange) +
                ", levelSearchRange=" + java.util.Arrays.toString(levelSearchRangeRange) +
                ", minLevelDistance=" + java.util.Arrays.toString(minLevelDistanceRange) +
                ", zoneRange=" + java.util.Arrays.toString(zoneRangeRange) +
                ", historyMonths=" + java.util.Arrays.toString(historyMonthsRange) +
                ", minPipsTP=" + java.util.Arrays.toString(minPipsTPRange) +
                ", minPipsSL=" + java.util.Arrays.toString(minPipsSLRange) +
                ", maxExposurePercent=" + java.util.Arrays.toString(maxExposurePercentRange) +
                ", tolerance=" + java.util.Arrays.toString(toleranceRange) +
                ", checkCounter=" + java.util.Arrays.toString(checkCounterRange) +
                ", currentLevelSearchRange=" + currentLevelSearchRange +
                '}';
    }
}