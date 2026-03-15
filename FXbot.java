package com.bot.fx;

import com.dukascopy.api.*;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;


public class FXbot implements IStrategy {
    private IConsole console;
    private IEngine engine;
    private IHistory history;
    private IContext context;
    private InstrumentSettings settings;
    
    private double accountBalance;
    private double totalProfit = 0.0; // To store total profit
    private double maxDrawdown = 0.0; // To store maximum drawdown
    private double peakEquity = 0.0; // To track peak equity for drawdown calculation
    private double pipValue;

    private double slPartialClosurePercentage;
    private double tpPartialClosurePercentage;
    
    private double latestTickPriceAsk;
    private double latestTickPriceBid;
    private int checkCounter;
    private int tradeCount;
    private boolean canOpenNewTrade = true;
    private double upperZone;
    private double lowerZone;
    private boolean recalculateLevels = true;
    private boolean isRunning = true;
    private boolean isInstrumentTradable = false;
    private long lastTradeTime = 0;

    private long id;

    public FXbot(InstrumentSettings settings) {
        this.id = System.currentTimeMillis();  // Assign unique ID based on current time
        this.settings = settings;
        this.slPartialClosurePercentage = settings.getSlPartialClosurePercentage();
        this.tpPartialClosurePercentage = settings.getTpPartialClosurePercentage();
        this.pipValue = settings.getPipValue();
    }

    public long getId() {
        return id;
    }
    
    @Override
    public void onStart(IContext context) throws JFException {
        this.console = context.getConsole();
        this.context = context;
        this.engine = context.getEngine();
        this.history = context.getHistory();
        console.getOut().println("Strategy started with settings: " + settings.toString());
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        try {
            if (!instrument.equals(settings.getInstrument()) || !isInstrumentTradable) {
                return;
            }

            long currentTime = tick.getTime();
            if (currentTime < settings.getStartTime() || currentTime > settings.getEndTime()) {
                return; // Exit if outside trading hours
            }

            latestTickPriceAsk = tick.getAsk();
            latestTickPriceBid = tick.getBid();
            console.getOut().println("Tick received - Ask: " + latestTickPriceAsk + ", Bid: " + latestTickPriceBid);
        } catch (Exception e) {
            console.getErr().println("Error in onTick: " + e.getMessage());
        }
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (!instrument.equals(settings.getInstrument())) return;

        List<IOrder> filledOrders = engine.getOrders(instrument).stream()
                .filter(o -> o.getState() == IOrder.State.FILLED)
                .collect(Collectors.toList());

        for (IOrder order : filledOrders) {
            manageTrade(order, bidBar.getClose());  // Use the close price from the bar instead of tick data
        }
    }
    
    @Override
    public void onMessage(IMessage message) throws JFException {
        try {
            if (message.getType() == IMessage.Type.INSTRUMENT_STATUS) {
                IInstrumentStatusMessage instrumentStatusMessage = (IInstrumentStatusMessage) message;
                Instrument messageInstrument = instrumentStatusMessage.getInstrument();
                if (messageInstrument != null && messageInstrument.equals(settings.getInstrument())) {
                    isInstrumentTradable = instrumentStatusMessage.isTradable();
                    console.getOut().println("Instrument " + settings.getInstrument() + " tradable: " + isInstrumentTradable);

                    if (isInstrumentTradable && recalculateLevels) {
                        console.getOut().println("Calculating initial levels...");
                        calculateLevels(); 
                        recalculateLevels = false;
                    }
                }
            }
            console.getOut().println("Received message: " + message);
        } catch (Exception e) {
            console.getErr().println("Error in onMessage: " + e.getMessage());
        }
    }

    private void calculateLevels() {
        upperZone = latestTickPriceAsk + settings.getCurrentLevelSearchRange();  // Use the current value
        lowerZone = latestTickPriceBid - settings.getCurrentLevelSearchRange();
        console.getOut().println("Levels calculated: Upper Zone = " + upperZone + ", Lower Zone = " + lowerZone);
    }

    @Override
    public void onAccount(IAccount account) throws JFException {
        this.accountBalance = account.getBalance();
        try {
            console.getOut().println("Received account update: " + account);

            double currentEquity = account.getEquity();
            this.accountBalance = account.getBalance();
            totalProfit = currentEquity - accountBalance;
            peakEquity = Math.max(peakEquity, currentEquity);
            maxDrawdown = Math.max(maxDrawdown, peakEquity - currentEquity);

            if (totalProfit < -settings.getMaxLossPercent() * accountBalance) {
                isRunning = false; // Stop trading if loss exceeds a certain percentage
            }
        } catch (Exception e) {
            console.getErr().println("Error in onAccount: " + e.getMessage());
        }
    }

    @Override
    public void onStop() throws JFException {
        console.getOut().println("Strategy stopped.");
    }
    
    private void manageTrade(IOrder order, double currentPrice) throws JFException {
        double entryPrice = order.getOpenPrice();
        double tpPrice = order.getTakeProfitPrice();
        double slPrice = order.getStopLossPrice();

        // Partial closure logic
        double slClosureLevel = slPrice + (entryPrice - slPrice) * slPartialClosurePercentage;
        double tpClosureLevel = tpPrice - (tpPrice - entryPrice) * tpPartialClosurePercentage;

        if (order.isLong()) {
            if (currentPrice <= slClosureLevel) {
                closePartialOrder(order, slPartialClosurePercentage);
            } else if (currentPrice >= tpClosureLevel) {
                closePartialOrder(order, tpPartialClosurePercentage);
            }
        } else {
            if (currentPrice >= slClosureLevel) {
                closePartialOrder(order, slPartialClosurePercentage);
            } else if (currentPrice <= tpClosureLevel) {
                closePartialOrder(order, tpPartialClosurePercentage);
            }
        }
    }

    public List<IBar> getBarsForInstrument(Instrument instrument, Period period, int numberOfBars) throws JFException {
        long endTime = context.getTime();
        long startTime = history.getTimeOfLastTick(instrument) - period.getInterval() * numberOfBars;
        return history.getBars(instrument, period, OfferSide.BID, startTime, endTime);
    }

    
    private void closePartialOrder(IOrder order, double percentage) throws JFException {
        double amountToClose = order.getAmount() * percentage;
        if (amountToClose > 0 && amountToClose < order.getAmount()) {
            order.close(amountToClose);
            console.getOut().println("Closed " + (percentage * 100) + "% of the trade.");
        } else if (amountToClose >= order.getAmount()) {
            order.close();
            console.getOut().println("Closed the entire trade.");
        }
    }
}