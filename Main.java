package com.bot.fx;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.Period;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
        IClient client = ClientFactory.getDefaultInstance();
        client.setSystemListener(new SystemListener());

        String jnlpUrl = "https://platform.dukascopy.com/demo/jforex.jnlp";
        String userName = "DEMO2FXiWM";
        String password = "FXiWM";

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

        List<InstrumentSettings> settingsList = initializeInstrumentSettings();
        long totalIterations = calculateTotalIterations(settingsList);
        long startTime = System.currentTimeMillis();
        long iterationsProcessed = 0;

        for (InstrumentSettings settings : settingsList) {
            try {
                iterationsProcessed++;
                
                IStrategy strategy = new FXbot(settings);
                // Start the strategy and obtain the ID from the IClient
                long strategyId = client.startStrategy(strategy);

                // Replace getId() with strategyId obtained from the client
                while (client.getStartedStrategies().containsKey(strategyId)) {
                    Thread.sleep(1000); // Wait for the strategy to finish
                }

                long elapsedTime = System.currentTimeMillis() - startTime;
                double estimatedTotalTime = (double) elapsedTime / iterationsProcessed * totalIterations;
                System.out.println("Processing iteration " + iterationsProcessed + " of " + totalIterations +
                        ". Estimated time remaining: " + ((estimatedTotalTime - elapsedTime) / 1000) + " seconds");

            } catch (Exception e) {
                System.err.println("Error during strategy execution: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Disconnect from the client after all strategies have finished processing
        client.disconnect();
    }

    private static long calculateTotalIterations(List<InstrumentSettings> settingsList) {
        long total = 0;
        for (InstrumentSettings settings : settingsList) {
            total += settings.getRiskPercentRange().length *
                    settings.getLevelSearchRangeRange().length *
                    settings.getMinLevelDistanceRange().length *
                    settings.getZoneRangeRange().length *
                    settings.getHistoryMonthsRange().length *
                    settings.getMinPipsTPRange().length *
                    settings.getMinPipsSLRange().length *
                    settings.getMaxExposurePercentRange().length *
                    settings.getToleranceRange().length *
                    settings.getCheckCounterRange().length;
        }
        return total;
    }

    private static List<InstrumentSettings> initializeInstrumentSettings() {
        List<InstrumentSettings> settingsList = new ArrayList<>();
        settingsList.add(new InstrumentSettings(
                Instrument.AUDUSD,
                0.0001,
                new double[]{0.5, 1, 2},
                new double[]{10, 20, 30},
                new double[]{1, 5, 10},
                new double[]{1, 2, 3},
                new int[]{1, 2},
                new double[]{10, 20},
                new double[]{5, 10},
                new double[]{2, 5},
                new double[]{1, 2},
                new int[]{1, 2, 3},
                Period.FOUR_HOURS,
                Period.ONE_HOUR,
                getTimeInMillis(9, 0),
                getTimeInMillis(17, 0),
                0.1,
                true,
                600000,
                0.5, // slPartialClosurePercentage
                0.8  // tpPartialClosurePercentage
        ));
        return settingsList;
    }

    private static long getTimeInMillis(int hours, int minutes) {
        return TimeUnit.HOURS.toMillis(hours) + TimeUnit.MINUTES.toMillis(minutes);
    }
}
