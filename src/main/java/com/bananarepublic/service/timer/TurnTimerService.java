package com.bananarepublic.service.timer;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TurnTimerService implements AutoCloseable {
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> activeTask;
    private int remainingSeconds;
    private Runnable onExpired;

    public TurnTimerService() {
        this(Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "turn-timer");
            thread.setDaemon(true);
            return thread;
        }));
    }

    TurnTimerService(ScheduledExecutorService scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "Scheduler cannot be null");
    }

    public synchronized void start(int seconds, Runnable onExpired) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Timer seconds cannot be negative");
        }

        stop();
        this.remainingSeconds = seconds;
        this.onExpired = Objects.requireNonNull(onExpired, "Expiration handler cannot be null");

        if (seconds == 0) {
            expireNow();
            return;
        }

        activeTask = scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (activeTask != null) {
            activeTask.cancel(false);
            activeTask = null;
        }
    }

    public synchronized int getRemainingSeconds() {
        return remainingSeconds;
    }

    public synchronized boolean isRunning() {
        return activeTask != null && !activeTask.isCancelled() && !activeTask.isDone();
    }

    public void expireNow() {
        Runnable callback;
        synchronized (this) {
            remainingSeconds = 0;
            stop();
            callback = onExpired;
        }

        if (callback != null) {
            callback.run();
        }
    }

    private void tick() {
        Runnable callback = null;

        synchronized (this) {
            if (remainingSeconds > 0) {
                remainingSeconds--;
            }

            if (remainingSeconds == 0) {
                stop();
                callback = onExpired;
            }
        }

        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public synchronized void close() {
        stop();
        scheduler.shutdownNow();
    }
}
