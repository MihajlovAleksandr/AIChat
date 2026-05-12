package com.example.aichat.model.connection;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReconnectController {

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final int maxAttempts;
    private final long baseDelayMillis;

    private int currentAttempt = 0;

    private final AtomicBoolean isActive = new AtomicBoolean(false);
    private final AtomicBoolean isRunningAttempt = new AtomicBoolean(false);

    private final ReconnectAction reconnectAction;
    private final Runnable onFailed;

    public interface ReconnectAction {
        CompletableFuture<Void> run();
    }

    public ReconnectController(
            int maxAttempts,
            long baseDelayMillis,
            ReconnectAction reconnectAction,
            Runnable onFailed
    ) {
        this.maxAttempts = maxAttempts;
        this.baseDelayMillis = baseDelayMillis;
        this.reconnectAction = reconnectAction;
        this.onFailed = onFailed;
    }

    public void start() {
        if (!isActive.compareAndSet(false, true)) {
            return;
        }

        scheduleNext();
    }

    public void stop() {
        isActive.set(false);
        isRunningAttempt.set(false);
        handler.removeCallbacksAndMessages(null);
        currentAttempt = 0;
    }

    public void reset() {
        currentAttempt = 0;
    }

    private void scheduleNext() {
        if (!isActive.get()) return;

        if (currentAttempt >= maxAttempts) {
            isActive.set(false);
            onFailed.run();
            return;
        }

        long delay = calculateDelay(currentAttempt);
        currentAttempt++;

        handler.postDelayed(this::executeAttempt, delay);
    }

    private void executeAttempt() {
        if (!isActive.get()) return;

        if (!isRunningAttempt.compareAndSet(false, true)) {
            return;
        }

        reconnectAction.run()
                .thenRun(() -> {
                    stop();
                })
                .exceptionally(ex -> {
                    isRunningAttempt.set(false);
                    scheduleNext();
                    return null;
                });
    }

    private long calculateDelay(int attempt) {
        return baseDelayMillis * (long) Math.pow(2, attempt);
    }
}