package com.bot.fx;

import com.dukascopy.api.system.ISystemListener;

public class SystemListener implements ISystemListener {
    @Override
    public void onStart(long processId) {
        System.out.println("Strategy started: " + processId);
    }

    @Override
    public void onStop(long processId) {
        System.out.println("Strategy stopped: " + processId);
    }

    @Override
    public void onConnect() {
        System.out.println("Connected to Dukascopy server.");
    }

    @Override
    public void onDisconnect() {
        System.out.println("Disconnected from Dukascopy server.");
    }
}
