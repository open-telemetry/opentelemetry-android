/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum;

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
