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

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

class RumAttributeAppender implements SpanProcessor {

    private final VisibleScreenTracker visibleScreenTracker;
    private final ConnectionUtil connectionUtil;
    private final CurrentNetworkAttributesExtractor networkAttributesExtractor =
            new CurrentNetworkAttributesExtractor();

    RumAttributeAppender(VisibleScreenTracker visibleScreenTracker, ConnectionUtil connectionUtil) {
        this.visibleScreenTracker = visibleScreenTracker;
        this.connectionUtil = connectionUtil;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        String currentScreen = visibleScreenTracker.getCurrentlyVisibleScreen();
        span.setAttribute(SplunkRum.SCREEN_NAME_KEY, currentScreen);

        CurrentNetwork currentNetwork = connectionUtil.getActiveNetwork();
        span.setAllAttributes(networkAttributesExtractor.extract(currentNetwork));
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {}

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
