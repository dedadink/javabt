package com.chad;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Results {

    public static void logResults(String strategyId, InstrumentSettings settings, double totalProfit, double drawdown, int numberOfTrades) {
        try (FileWriter writer = new FileWriter("backtest_results.csv", true)) {
            writer.append(strategyId)
                .append(',')
                .append(Double.toString(totalProfit))
                .append(',')
                .append(Double.toString(drawdown))
                .append(',')
                .append(Integer.toString(numberOfTrades))
                .append(',')
                .append(Double.toString(settings.getPipValue()))
                .append(',')
                .append(Double.toString(settings.getRiskPercentRange()[0]))
                .append(',')
                .append(Double.toString(settings.getLevelSearchRangeRange()[0]))
                .append(',')
                .append(Double.toString(settings.getMinLevelDistanceRange()[0]))
                .append(',')
                .append(Double.toString(settings.getZoneRangeRange()[0]))
                .append(',')
                .append(Double.toString(settings.getMinPipsTPRange()[0]))
                .append(',')
                .append(Double.toString(settings.getMinPipsSLRange()[0]))
                .append(',')
                .append(Double.toString(settings.getMaxExposurePercentRange()[0]))
                .append(',')
                .append(Double.toString(settings.getToleranceRange()[0]))
                .append(',')
                .append(Integer.toString(settings.getCheckCounterRange()[0]))
                .append(',')
                .append(Boolean.toString(settings.isWaitForNextCandle()))
                .append(',')
                .append(Long.toString(settings.getPostTradeWaitTime()))
                .append('\n');
        } catch (IOException e) {
            System.err.println("Error logging results: " + e.getMessage());
        }
    }

    // Analyze results from the CSV file
    public static void analyzeResults() {
        try (BufferedReader reader = new BufferedReader(new FileReader("backtest_results.csv"))) {
            String line;
            List<Result> results = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                results.add(new Result(
                        parts[0], 
                        Double.parseDouble(parts[1]), 
                        Double.parseDouble(parts[2]), 
                        Integer.parseInt(parts[3]),
                        parts[4], // pipValue
                        parts[5], // riskPercent
                        parts[6], // levelSearchRange
                        parts[7], // minLevelDistance
                        parts[8], // zoneRange
                        parts[9], // minPipsTP
                        parts[10], // minPipsSL
                        parts[11], // maxExposurePercent
                        parts[12], // tolerance
                        parts[13], // checkCounter
                        parts[14], // waitForNextCandle
                        parts[15]  // postTradeWaitTime
                ));
            }

            results.sort(Comparator.comparingDouble(Result::getTotalProfit).reversed());

            System.out.println("\nTop Backtest Results:");
            for (int i = 0; i < Math.min(10, results.size()); i++) {
                Result result = results.get(i);
                System.out.println(result);
                System.out.println("------------------------");
            }
        } catch (IOException e) {
            System.err.println("Error analyzing results: " + e.getMessage());
        }
    }
}

class Result {
    private String strategyId;
    private double totalProfit;
    private double drawdown;
    private int numberOfTrades;
    private String pipValue;
    private String riskPercent;
    private String levelSearchRange;
    private String minLevelDistance;
    private String zoneRange;
    private String minPipsTP;
    private String minPipsSL;
    private String maxExposurePercent;
    private String tolerance;
    private String checkCounter;
    private String waitForNextCandle;
    private String postTradeWaitTime;

    public Result(String strategyId, double totalProfit, double drawdown, int numberOfTrades, 
                  String pipValue, String riskPercent, String levelSearchRange, String minLevelDistance, 
                  String zoneRange, String minPipsTP, String minPipsSL, String maxExposurePercent, 
                  String tolerance, String checkCounter, String waitForNextCandle, String postTradeWaitTime) {
        this.strategyId = strategyId;
        this.totalProfit = totalProfit;
        this.drawdown = drawdown;
        this.numberOfTrades = numberOfTrades;
        this.pipValue = pipValue;
        this.riskPercent = riskPercent;
        this.levelSearchRange = levelSearchRange;
        this.minLevelDistance = minLevelDistance;
        this.zoneRange = zoneRange;
        this.minPipsTP = minPipsTP;
        this.minPipsSL = minPipsSL;
        this.maxExposurePercent = maxExposurePercent;
        this.tolerance = tolerance;
        this.checkCounter = checkCounter;
        this.waitForNextCandle = waitForNextCandle;
        this.postTradeWaitTime = postTradeWaitTime;
    }

    public String getId() {
        return strategyId;
    }

    public double getTotalProfit() {
        return totalProfit;
    }

    public double getDrawdown() {
        return drawdown;
    }

    public int getNumberOfTrades() {
        return numberOfTrades;
    }

    @Override
    public String toString() {
        return "Strategy ID: " + strategyId +
                ", Total Profit: " + totalProfit +
                ", Drawdown: " + drawdown +
                ", Number of Trades: " + numberOfTrades +
                ", Pip Value: " + pipValue +
                ", Risk Percent: " + riskPercent +
                ", Level Search Range: " + levelSearchRange +
                ", Min Level Distance: " + minLevelDistance +
                ", Zone Range: " + zoneRange +
                ", Min Pips TP: " + minPipsTP +
                ", Min Pips SL: " + minPipsSL +
                ", Max Exposure Percent: " + maxExposurePercent +
                ", Tolerance: " + tolerance +
                ", Check Counter: " + checkCounter +
                ", Wait For Next Candle: " + waitForNextCandle +
                ", Post Trade Wait Time: " + postTradeWaitTime;
    }
}
