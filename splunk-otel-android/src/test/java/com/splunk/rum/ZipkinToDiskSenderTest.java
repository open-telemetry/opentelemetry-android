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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.common.Clock;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZipkinToDiskSenderTest {

    private final long now = System.currentTimeMillis();
    private final File path = new File("/my/great/storage/location");
    private final String finalFile = "/my/great/storage/location/" + now + ".spans";
    private final File finalPath = new File(finalFile);
    private final byte[] span1 = "span one".getBytes(StandardCharsets.UTF_8);
    private final byte[] span2 = "span one".getBytes(StandardCharsets.UTF_8);
    private final List<byte[]> spans = Arrays.asList(span1, span2);

    @Mock private FileUtils fileUtils;
    @Mock private Clock clock;
    @Mock private DeviceSpanStorageLimiter limiter;

    @BeforeEach
    void setup() {
        when(clock.now()).thenReturn(now);
        when(limiter.ensureFreeSpace()).thenReturn(true);
    }

    @Test
    void testHappyPath() throws Exception {

        ZipkinToDiskSender sender =
                ZipkinToDiskSender.builder()
                        .path(path)
                        .fileUtils(fileUtils)
                        .clock(clock)
                        .storageLimiter(limiter)
                        .build();
        sender.sendSpans(spans);

        verify(fileUtils).writeAsLines(finalPath, spans);
    }

    @Test
    void testWriteFails() throws Exception {
        doThrow(new IOException("boom")).when(fileUtils).writeAsLines(finalPath, spans);

        ZipkinToDiskSender sender =
                ZipkinToDiskSender.builder()
                        .path(path)
                        .fileUtils(fileUtils)
                        .clock(clock)
                        .storageLimiter(limiter)
                        .build();

        sender.sendSpans(spans);
        // Exception not thrown
    }

    @Test
    void testLimitExceeded() {
        Mockito.reset(clock);
        when(limiter.ensureFreeSpace()).thenReturn(false);

        ZipkinToDiskSender sender =
                ZipkinToDiskSender.builder()
                        .path(path)
                        .fileUtils(fileUtils)
                        .clock(clock)
                        .storageLimiter(limiter)
                        .build();

        sender.sendSpans(spans);

        verifyNoMoreInteractions(clock);
        verifyNoMoreInteractions(fileUtils);
    }
}
