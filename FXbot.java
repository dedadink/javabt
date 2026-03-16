diff --git a/FXbot.java b/FXbot.java
index 2536f000b7469518f63f8a3a5e902653fa9bcb87..cc305da203d2bfaa392a7a3be2fa3e963f05ff6f 100644
--- a/FXbot.java
+++ b/FXbot.java
@@ -1,195 +1,222 @@
-package com.bot.fx;
-
-import com.dukascopy.api.*;
-import java.util.Set;
-import java.util.HashSet;
-import java.util.List;
-import java.text.SimpleDateFormat;
-import java.util.Date;
-import java.util.UUID;
-import java.util.concurrent.TimeUnit;
-import java.util.ArrayList;
-import java.util.Map;
-import java.util.stream.Collectors;
-
-
-public class FXbot implements IStrategy {
-    private IConsole console;
-    private IEngine engine;
-    private IHistory history;
-    private IContext context;
-    private InstrumentSettings settings;
-    
-    private double accountBalance;
-    private double totalProfit = 0.0; // To store total profit
-    private double maxDrawdown = 0.0; // To store maximum drawdown
-    private double peakEquity = 0.0; // To track peak equity for drawdown calculation
-    private double pipValue;
-
-    private double slPartialClosurePercentage;
-    private double tpPartialClosurePercentage;
-    
-    private double latestTickPriceAsk;
-    private double latestTickPriceBid;
-    private int checkCounter;
-    private int tradeCount;
-    private boolean canOpenNewTrade = true;
-    private double upperZone;
-    private double lowerZone;
-    private boolean recalculateLevels = true;
-    private boolean isRunning = true;
-    private boolean isInstrumentTradable = false;
-    private long lastTradeTime = 0;
-
-    private long id;
-
-    public FXbot(InstrumentSettings settings) {
-        this.id = System.currentTimeMillis();  // Assign unique ID based on current time
-        this.settings = settings;
-        this.slPartialClosurePercentage = settings.getSlPartialClosurePercentage();
-        this.tpPartialClosurePercentage = settings.getTpPartialClosurePercentage();
-        this.pipValue = settings.getPipValue();
-    }
-
-    public long getId() {
-        return id;
-    }
-    
-    @Override
-    public void onStart(IContext context) throws JFException {
-        this.console = context.getConsole();
-        this.context = context;
-        this.engine = context.getEngine();
-        this.history = context.getHistory();
-        console.getOut().println("Strategy started with settings: " + settings.toString());
-    }
-
-    @Override
-    public void onTick(Instrument instrument, ITick tick) throws JFException {
-        try {
-            if (!instrument.equals(settings.getInstrument()) || !isInstrumentTradable) {
-                return;
-            }
-
-            long currentTime = tick.getTime();
-            if (currentTime < settings.getStartTime() || currentTime > settings.getEndTime()) {
-                return; // Exit if outside trading hours
-            }
-
-            latestTickPriceAsk = tick.getAsk();
-            latestTickPriceBid = tick.getBid();
-            console.getOut().println("Tick received - Ask: " + latestTickPriceAsk + ", Bid: " + latestTickPriceBid);
-        } catch (Exception e) {
-            console.getErr().println("Error in onTick: " + e.getMessage());
-        }
-    }
-
-    @Override
-    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
-        if (!instrument.equals(settings.getInstrument())) return;
-
-        List<IOrder> filledOrders = engine.getOrders(instrument).stream()
-                .filter(o -> o.getState() == IOrder.State.FILLED)
-                .collect(Collectors.toList());
-
-        for (IOrder order : filledOrders) {
-            manageTrade(order, bidBar.getClose());  // Use the close price from the bar instead of tick data
-        }
-    }
-    
-    @Override
-    public void onMessage(IMessage message) throws JFException {
-        try {
-            if (message.getType() == IMessage.Type.INSTRUMENT_STATUS) {
-                IInstrumentStatusMessage instrumentStatusMessage = (IInstrumentStatusMessage) message;
-                Instrument messageInstrument = instrumentStatusMessage.getInstrument();
-                if (messageInstrument != null && messageInstrument.equals(settings.getInstrument())) {
-                    isInstrumentTradable = instrumentStatusMessage.isTradable();
-                    console.getOut().println("Instrument " + settings.getInstrument() + " tradable: " + isInstrumentTradable);
-
-                    if (isInstrumentTradable && recalculateLevels) {
-                        console.getOut().println("Calculating initial levels...");
-                        calculateLevels(); 
-                        recalculateLevels = false;
-                    }
-                }
-            }
-            console.getOut().println("Received message: " + message);
-        } catch (Exception e) {
-            console.getErr().println("Error in onMessage: " + e.getMessage());
-        }
-    }
-
-    private void calculateLevels() {
-        upperZone = latestTickPriceAsk + settings.getCurrentLevelSearchRange();  // Use the current value
-        lowerZone = latestTickPriceBid - settings.getCurrentLevelSearchRange();
-        console.getOut().println("Levels calculated: Upper Zone = " + upperZone + ", Lower Zone = " + lowerZone);
-    }
-
-    @Override
-    public void onAccount(IAccount account) throws JFException {
-        this.accountBalance = account.getBalance();
-        try {
-            console.getOut().println("Received account update: " + account);
-
-            double currentEquity = account.getEquity();
-            this.accountBalance = account.getBalance();
-            totalProfit = currentEquity - accountBalance;
-            peakEquity = Math.max(peakEquity, currentEquity);
-            maxDrawdown = Math.max(maxDrawdown, peakEquity - currentEquity);
-
-            if (totalProfit < -settings.getMaxLossPercent() * accountBalance) {
-                isRunning = false; // Stop trading if loss exceeds a certain percentage
-            }
-        } catch (Exception e) {
-            console.getErr().println("Error in onAccount: " + e.getMessage());
-        }
-    }
-
-    @Override
-    public void onStop() throws JFException {
-        console.getOut().println("Strategy stopped.");
-    }
-    
-    private void manageTrade(IOrder order, double currentPrice) throws JFException {
-        double entryPrice = order.getOpenPrice();
-        double tpPrice = order.getTakeProfitPrice();
-        double slPrice = order.getStopLossPrice();
-
-        // Partial closure logic
-        double slClosureLevel = slPrice + (entryPrice - slPrice) * slPartialClosurePercentage;
-        double tpClosureLevel = tpPrice - (tpPrice - entryPrice) * tpPartialClosurePercentage;
-
-        if (order.isLong()) {
-            if (currentPrice <= slClosureLevel) {
-                closePartialOrder(order, slPartialClosurePercentage);
-            } else if (currentPrice >= tpClosureLevel) {
-                closePartialOrder(order, tpPartialClosurePercentage);
-            }
-        } else {
-            if (currentPrice >= slClosureLevel) {
-                closePartialOrder(order, slPartialClosurePercentage);
-            } else if (currentPrice <= tpClosureLevel) {
-                closePartialOrder(order, tpPartialClosurePercentage);
-            }
-        }
-    }
-
-    public List<IBar> getBarsForInstrument(Instrument instrument, Period period, int numberOfBars) throws JFException {
-        long endTime = context.getTime();
-        long startTime = history.getTimeOfLastTick(instrument) - period.getInterval() * numberOfBars;
-        return history.getBars(instrument, period, OfferSide.BID, startTime, endTime);
-    }
-
-    
-    private void closePartialOrder(IOrder order, double percentage) throws JFException {
-        double amountToClose = order.getAmount() * percentage;
-        if (amountToClose > 0 && amountToClose < order.getAmount()) {
-            order.close(amountToClose);
-            console.getOut().println("Closed " + (percentage * 100) + "% of the trade.");
-        } else if (amountToClose >= order.getAmount()) {
-            order.close();
-            console.getOut().println("Closed the entire trade.");
-        }
-    }
-}
\ No newline at end of file
+package com.bot.fx;
+
+import com.dukascopy.api.IAccount;
+import com.dukascopy.api.IBar;
+import com.dukascopy.api.IConsole;
+import com.dukascopy.api.IContext;
+import com.dukascopy.api.IEngine;
+import com.dukascopy.api.IHistory;
+import com.dukascopy.api.IInstrumentStatusMessage;
+import com.dukascopy.api.IMessage;
+import com.dukascopy.api.IOrder;
+import com.dukascopy.api.IStrategy;
+import com.dukascopy.api.ITick;
+import com.dukascopy.api.Instrument;
+import com.dukascopy.api.JFException;
+import com.dukascopy.api.OfferSide;
+import com.dukascopy.api.Period;
+
+import java.time.Instant;
+import java.time.LocalTime;
+import java.time.ZoneOffset;
+import java.util.List;
+import java.util.stream.Collectors;
+
+public class FXbot implements IStrategy {
+    private static final ZoneOffset TRADING_TIMEZONE = ZoneOffset.UTC;
+
+    private IConsole console;
+    private IEngine engine;
+    private IHistory history;
+    private IContext context;
+    private final InstrumentSettings settings;
+
+    private double accountBalance;
+    private double totalProfit = 0.0;
+    private double maxDrawdown = 0.0;
+    private double peakEquity = 0.0;
+
+    private final double slPartialClosurePercentage;
+    private final double tpPartialClosurePercentage;
+
+    private double latestTickPriceAsk;
+    private double latestTickPriceBid;
+    private double upperZone;
+    private double lowerZone;
+    private boolean recalculateLevels = true;
+    private boolean isRunning = true;
+    private boolean isInstrumentTradable = false;
+
+    public FXbot(InstrumentSettings settings) {
+        this.settings = settings;
+        this.slPartialClosurePercentage = settings.getSlPartialClosurePercentage();
+        this.tpPartialClosurePercentage = settings.getTpPartialClosurePercentage();
+    }
+
+    @Override
+    public void onStart(IContext context) throws JFException {
+        this.console = context.getConsole();
+        this.context = context;
+        this.engine = context.getEngine();
+        this.history = context.getHistory();
+
+        if (settings.getCurrentLevelSearchRange() <= 0 && settings.getLevelSearchRangeRange().length > 0) {
+            settings.setCurrentLevelSearchRange(settings.getLevelSearchRangeRange()[0]);
+        }
+
+        if (settings.getCurrentLevelSearchRange() <= 0) {
+            throw new IllegalStateException("Current level search range must be greater than zero.");
+        }
+
+        console.getOut().println("Strategy started with settings: " + settings.toString());
+    }
+
+    @Override
+    public void onTick(Instrument instrument, ITick tick) throws JFException {
+        try {
+            if (!isRunning || !instrument.equals(settings.getInstrument()) || !isInstrumentTradable) {
+                return;
+            }
+
+            if (!isWithinTradingWindow(tick.getTime(), settings.getStartTime(), settings.getEndTime())) {
+                return;
+            }
+
+            latestTickPriceAsk = tick.getAsk();
+            latestTickPriceBid = tick.getBid();
+        } catch (Exception e) {
+            console.getErr().println("Error in onTick: " + e.getMessage());
+        }
+    }
+
+    @Override
+    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
+        if (!isRunning || !instrument.equals(settings.getInstrument())) {
+            return;
+        }
+
+        List<IOrder> filledOrders = engine.getOrders(instrument).stream()
+                .filter(o -> o.getState() == IOrder.State.FILLED)
+                .collect(Collectors.toList());
+
+        for (IOrder order : filledOrders) {
+            manageTrade(order, bidBar.getClose());
+        }
+    }
+
+    @Override
+    public void onMessage(IMessage message) throws JFException {
+        try {
+            if (message.getType() == IMessage.Type.INSTRUMENT_STATUS) {
+                IInstrumentStatusMessage instrumentStatusMessage = (IInstrumentStatusMessage) message;
+                Instrument messageInstrument = instrumentStatusMessage.getInstrument();
+                if (messageInstrument != null && messageInstrument.equals(settings.getInstrument())) {
+                    isInstrumentTradable = instrumentStatusMessage.isTradable();
+                    console.getOut().println(
+                            "Instrument " + settings.getInstrument() + " tradable: " + isInstrumentTradable);
+
+                    if (isInstrumentTradable && recalculateLevels) {
+                        calculateLevels();
+                        recalculateLevels = false;
+                    }
+                }
+            }
+        } catch (Exception e) {
+            console.getErr().println("Error in onMessage: " + e.getMessage());
+        }
+    }
+
+    private void calculateLevels() {
+        upperZone = latestTickPriceAsk + settings.getCurrentLevelSearchRange();
+        lowerZone = latestTickPriceBid - settings.getCurrentLevelSearchRange();
+        console.getOut().println("Levels calculated: Upper Zone = " + upperZone + ", Lower Zone = " + lowerZone);
+    }
+
+    @Override
+    public void onAccount(IAccount account) throws JFException {
+        this.accountBalance = account.getBalance();
+        try {
+            double currentEquity = account.getEquity();
+            this.accountBalance = account.getBalance();
+            totalProfit = currentEquity - accountBalance;
+            peakEquity = Math.max(peakEquity, currentEquity);
+            maxDrawdown = Math.max(maxDrawdown, peakEquity - currentEquity);
+
+            if (totalProfit < -settings.getMaxLossPercent() * accountBalance) {
+                isRunning = false;
+                console.getWarn().println("Max loss reached, closing orders and stopping strategy logic.");
+                closeOpenOrders();
+            }
+        } catch (Exception e) {
+            console.getErr().println("Error in onAccount: " + e.getMessage());
+        }
+    }
+
+    @Override
+    public void onStop() throws JFException {
+        console.getOut().println("Strategy stopped. Profit=" + totalProfit + ", maxDrawdown=" + maxDrawdown
+                + ", upperZone=" + upperZone + ", lowerZone=" + lowerZone);
+    }
+
+    private void manageTrade(IOrder order, double currentPrice) throws JFException {
+        double entryPrice = order.getOpenPrice();
+        double tpPrice = order.getTakeProfitPrice();
+        double slPrice = order.getStopLossPrice();
+
+        double slClosureLevel = slPrice + (entryPrice - slPrice) * slPartialClosurePercentage;
+        double tpClosureLevel = tpPrice - (tpPrice - entryPrice) * tpPartialClosurePercentage;
+
+        if (order.isLong()) {
+            if (currentPrice <= slClosureLevel) {
+                closePartialOrder(order, slPartialClosurePercentage);
+            } else if (currentPrice >= tpClosureLevel) {
+                closePartialOrder(order, tpPartialClosurePercentage);
+            }
+        } else {
+            if (currentPrice >= slClosureLevel) {
+                closePartialOrder(order, slPartialClosurePercentage);
+            } else if (currentPrice <= tpClosureLevel) {
+                closePartialOrder(order, tpPartialClosurePercentage);
+            }
+        }
+    }
+
+    public List<IBar> getBarsForInstrument(Instrument instrument, Period period, int numberOfBars) throws JFException {
+        long endTime = context.getTime();
+        long startTime = history.getTimeOfLastTick(instrument) - period.getInterval() * numberOfBars;
+        return history.getBars(instrument, period, OfferSide.BID, startTime, endTime);
+    }
+
+    private void closePartialOrder(IOrder order, double percentage) throws JFException {
+        double amountToClose = order.getAmount() * percentage;
+        if (amountToClose > 0 && amountToClose < order.getAmount()) {
+            order.close(amountToClose);
+            console.getOut().println("Closed " + (percentage * 100) + "% of the trade.");
+        } else if (amountToClose >= order.getAmount()) {
+            order.close();
+            console.getOut().println("Closed the entire trade.");
+        }
+    }
+
+    private void closeOpenOrders() throws JFException {
+        for (IOrder order : engine.getOrders(settings.getInstrument())) {
+            if (order.getState() != IOrder.State.CLOSED && order.getState() != IOrder.State.CANCELED) {
+                order.close();
+            }
+        }
+    }
+
+    private boolean isWithinTradingWindow(long epochMillis, long startTimeMillisOfDay, long endTimeMillisOfDay) {
+        long millisOfDay = toMillisOfDay(epochMillis);
+        if (startTimeMillisOfDay <= endTimeMillisOfDay) {
+            return millisOfDay >= startTimeMillisOfDay && millisOfDay <= endTimeMillisOfDay;
+        }
+
+        return millisOfDay >= startTimeMillisOfDay || millisOfDay <= endTimeMillisOfDay;
+    }
+
+    private long toMillisOfDay(long epochMillis) {
+        LocalTime time = Instant.ofEpochMilli(epochMillis).atZone(TRADING_TIMEZONE).toLocalTime();
+        return time.toNanoOfDay() / 1_000_000;
+    }
+}
