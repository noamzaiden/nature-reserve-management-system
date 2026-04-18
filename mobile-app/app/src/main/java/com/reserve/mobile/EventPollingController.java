package com.reserve.mobile;

import android.os.Handler;
import android.os.Looper;

import java.util.function.BooleanSupplier;

final class EventPollingController {

    private final long pollIntervalMs;
    private final BooleanSupplier shouldPollNow;
    private final Runnable pollAction;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean running;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            if (shouldPollNow.getAsBoolean()) {
                pollAction.run();
            }
            handler.postDelayed(this, pollIntervalMs);
        }
    };

    // Keeps periodic hazard polling logic out of MainActivity.
    EventPollingController(long pollIntervalMs,
                           BooleanSupplier shouldPollNow,
                           Runnable pollAction) {
        this.pollIntervalMs = pollIntervalMs;
        this.shouldPollNow = shouldPollNow;
        this.pollAction = pollAction;
    }

    // Starts polling loop if it is not already running.
    void start() {
        if (running) {
            return;
        }
        running = true;
        handler.postDelayed(pollRunnable, pollIntervalMs);
    }

    // Stops polling and removes pending callbacks.
    void stop() {
        running = false;
        handler.removeCallbacks(pollRunnable);
    }
}

