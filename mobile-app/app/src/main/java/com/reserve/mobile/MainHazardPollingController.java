package com.reserve.mobile;

import android.os.Handler;
import android.os.Looper;

final class MainHazardPollingController {

    interface ShouldPollCallback {
        boolean shouldPollNow();
    }

    interface PollActionCallback {
        void runPollAction();
    }

    private final long pollIntervalMs;
    private final ShouldPollCallback shouldPollCallback;
    private final PollActionCallback pollActionCallback;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean running;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            if (shouldPollCallback.shouldPollNow()) {
                pollActionCallback.runPollAction();
            }
            handler.postDelayed(this, pollIntervalMs);
        }
    };

    // Keeps periodic hazard polling logic out of MainActivity.
    MainHazardPollingController(long pollIntervalMs,
                                ShouldPollCallback shouldPollCallback,
                                PollActionCallback pollActionCallback) {
        this.pollIntervalMs = pollIntervalMs;
        this.shouldPollCallback = shouldPollCallback;
        this.pollActionCallback = pollActionCallback;
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

