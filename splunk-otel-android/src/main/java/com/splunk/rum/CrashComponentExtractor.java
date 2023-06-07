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

import static com.splunk.rum.SplunkRum.COMPONENT_KEY;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.rum.internal.instrumentation.crash.CrashDetails;
import java.util.concurrent.atomic.AtomicBoolean;

final class CrashComponentExtractor implements AttributesExtractor<CrashDetails, Void> {

    private final AtomicBoolean crashHappened = new AtomicBoolean(false);

    @Override
    public void onStart(
            AttributesBuilder attributes, Context parentContext, CrashDetails crashDetails) {
        // the idea here is to set component=crash only for the first error that arrives here
        // when multiple threads fail at roughly the same time (e.g. because of an OOM error),
        // the first error to arrive here is actually responsible for crashing the app; and all
        // the others that are captured before OS actually kills the process are just additional
        // info (component=error)
        String component =
                crashHappened.compareAndSet(false, true)
                        ? SplunkRum.COMPONENT_CRASH
                        : SplunkRum.COMPONENT_ERROR;
        attributes.put(COMPONENT_KEY, component);
    }

    @Override
    public void onEnd(
            AttributesBuilder attributes,
            Context context,
            CrashDetails crashDetails,
            Void unused,
            Throwable error) {}
}
