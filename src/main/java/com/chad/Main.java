package com.chad;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Legacy launcher retained for compatibility.
 *
 * V1 runtime is backtest-only; this class now delegates to BacktestMain.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("Main is now delegated to backtest-only path (BacktestMain).");
        BacktestMain.main(args);
    }

    static List<InstrumentSettings> initializeInstrumentSettingsForBacktest() {
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