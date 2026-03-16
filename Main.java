package com.bot.fx;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
        IClient client = ClientFactory.getDefaultInstance();
        client.setSystemListener(new SystemListener());

        String jnlpUrl = "https://platform.dukascopy.com/demo/jforex.jnlp";
        String userName = requireEnv("DUKASCOPY_USER");
        String password = requireEnv("DUKASCOPY_PASSWORD");

        System.out.println("Connecting...");
        client.connect(jnlpUrl, userName, password);

        int attempts = 10;
        while (attempts > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            attempts--;
        }
        if (!client.isConnected()) {
            System.err.println("Failed to connect to Dukascopy servers.");
            System.exit(1);
        }

        List<InstrumentSettings> baseSettingsList = initializeInstrumentSettings();
        List<InstrumentSettings> settingsRuns = expandSettings(baseSettingsList);
        long totalIterations = settingsRuns.size();
        long startTime = System.currentTimeMillis();
        long iterationsProcessed = 0;

        for (InstrumentSettings settings : settingsRuns) {
            try {
                iterationsProcessed++;

                IStrategy strategy = new FXbot(settings);
                long strategyId = client.startStrategy(strategy);

                while (client.getStartedStrategies().containsKey(strategyId)) {
                    Thread.sleep(1000);
                }

                long elapsedTime = System.currentTimeMillis() - startTime;
                double estimatedTotalTime = (double) elapsedTime / iterationsProcessed * totalIterations;
                System.out.println(
                        "Processing iteration " + iterationsProcessed + " of " + totalIterations
                                + ". Estimated time remaining: " + ((estimatedTotalTime - elapsedTime) / 1000)
                                + " seconds");

            } catch (Exception e) {
                System.err.println("Error during strategy execution: " + e.getMessage());
                e.printStackTrace();
            }
        }

        client.disconnect();
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value;
    }

    private static List<InstrumentSettings> expandSettings(List<InstrumentSettings> settingsList) {
        List<InstrumentSettings> expanded = new ArrayList<>();

        for (InstrumentSettings base : settingsList) {
            for (double risk : base.getRiskPercentRange()) {
                for (double levelSearch : base.getLevelSearchRangeRange()) {
                    for (double minLevelDistance : base.getMinLevelDistanceRange()) {
                        for (double zoneRange : base.getZoneRangeRange()) {
                            for (int historyMonths : base.getHistoryMonthsRange()) {
                                for (double minPipsTP : base.getMinPipsTPRange()) {
                                    for (double minPipsSL : base.getMinPipsSLRange()) {
                                        for (double maxExposure : base.getMaxExposurePercentRange()) {
                                            for (double tolerance : base.getToleranceRange()) {
                                                for (int checkCounter : base.getCheckCounterRange()) {
                                                    InstrumentSettings run = new InstrumentSettings(
                                                            base.getInstrument(),
                                                            base.getPipValue(),
                                                            new double[] { risk },
                                                            new double[] { levelSearch },
                                                            new double[] { minLevelDistance },
                                                            new double[] { zoneRange },
                                                            new int[] { historyMonths },
                                                            new double[] { minPipsTP },
                                                            new double[] { minPipsSL },
                                                            new double[] { maxExposure },
                                                            new double[] { tolerance },
                                                            new int[] { checkCounter },
                                                            base.getLevelCalculationPeriod(),
                                                            base.getTradeExecutionPeriod(),
                                                            base.getStartTime(),
                                                            base.getEndTime(),
                                                            base.getMaxLossPercent(),
                                                            base.isWaitForNextCandle(),
                                                            base.getPostTradeWaitTime(),
                                                            base.getSlPartialClosurePercentage(),
                                                            base.getTpPartialClosurePercentage());
                                                    run.setCurrentLevelSearchRange(levelSearch);
                                                    expanded.add(run);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return expanded;
    }

    private static List<InstrumentSettings> initializeInstrumentSettings() {
        List<InstrumentSettings> settingsList = new ArrayList<>();
        settingsList.add(new InstrumentSettings(
                Instrument.AUDUSD,
                0.0001,
                new double[] { 0.5, 1, 2 },
                new double[] { 10, 20, 30 },
                new double[] { 1, 5, 10 },
                new double[] { 1, 2, 3 },
                new int[] { 1, 2 },
                new double[] { 10, 20 },
                new double[] { 5, 10 },
                new double[] { 2, 5 },
                new double[] { 1, 2 },
                new int[] { 1, 2, 3 },
                Period.FOUR_HOURS,
                Period.ONE_HOUR,
                getTimeInMillis(9, 0),
                getTimeInMillis(17, 0),
                0.1,
                true,
                600000,
                0.5,
                0.8));
        return settingsList;
    }

    private static long getTimeInMillis(int hours, int minutes) {
        return TimeUnit.HOURS.toMillis(hours) + TimeUnit.MINUTES.toMillis(minutes);
    }
}
