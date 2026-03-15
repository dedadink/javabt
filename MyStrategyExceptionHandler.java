package com.bot.fx;

import com.dukascopy.api.system.IStrategyExceptionHandler;

public class MyStrategyExceptionHandler implements IStrategyExceptionHandler {

    @Override
    public void onException(long strategyId, Source source, Throwable throwable) {
        System.err.println("Error in strategy with ID: " + strategyId + " Source: " + source);
        throwable.printStackTrace();
    }
}
