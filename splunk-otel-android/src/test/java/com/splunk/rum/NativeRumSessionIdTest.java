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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class NativeRumSessionIdTest {

    @Test
    void getNativeSessionId() {
        SplunkRum splunkRum = mock(SplunkRum.class);
        when(splunkRum.getRumSessionId()).thenReturn("123456");

        NativeRumSessionId nativeRumSessionId = new NativeRumSessionId(splunkRum);

        String nativeSessionId = nativeRumSessionId.getNativeSessionId();
        assertEquals("123456", nativeSessionId);
    }
}
