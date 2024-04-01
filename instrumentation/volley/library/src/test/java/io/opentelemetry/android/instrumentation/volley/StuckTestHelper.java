/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.volley;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StuckTestHelper implements AutoCloseable {
    private final ScheduledExecutorService scheduledExecutorService;

    private StuckTestHelper() {
        Runnable threadDump =
                () -> {
                    System.err.println("---------------");
                    for (Map.Entry<Thread, StackTraceElement[]> entry :
                            Thread.getAllStackTraces().entrySet()) {
                        System.err.println(entry.getKey());
                        for (StackTraceElement stackTraceElement : entry.getValue()) {
                            System.err.println(stackTraceElement);
                        }
                        System.err.println();
                    }
                };
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(threadDump, 1, 1, TimeUnit.MINUTES);
    }

    public static StuckTestHelper start() {
        return new StuckTestHelper();
    }

    @Override
    public void close() {
        scheduledExecutorService.shutdown();
    }
}
