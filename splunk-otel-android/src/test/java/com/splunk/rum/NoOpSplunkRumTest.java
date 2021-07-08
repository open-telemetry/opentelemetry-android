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

import org.junit.Test;

import io.opentelemetry.api.common.Attributes;

import static org.junit.Assert.assertNotNull;

public class NoOpSplunkRumTest {

    @Test
    public void doesNotThrow() {
        NoOpSplunkRum instance = NoOpSplunkRum.INSTANCE;
        instance.addRumEvent("foo", Attributes.empty());
        instance.addRumException("bar", Attributes.empty(), new RuntimeException());
        assertNotNull(instance.createOkHttpRumInterceptor());
        assertNotNull(instance.getOpenTelemetry());
        assertNotNull(instance.getRumSessionId());
        instance.flushSpans();
    }
}