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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static java.util.Collections.emptyList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import zipkin2.Call;
import zipkin2.reporter.Sender;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class FileSenderTest {

    private final byte[] span1 = "span1".getBytes(StandardCharsets.UTF_8);
    private final byte[] span2 = "span2".getBytes(StandardCharsets.UTF_8);
    private final byte[] span3 = "span3".getBytes(StandardCharsets.UTF_8);
    private final File file = new File("meep");
    private final List<byte[]> fileSpans = Arrays.asList(span1, span2, span3);

    @Mock private FileUtils fileUtils;
    @Mock private BandwidthTracker bandwidthTracker;
    @Mock private Sender delegate;
    @Mock private Call<Void> httpCall;
    @Mock private Consumer<Integer> backoff;

    @BeforeEach
    void setup() throws Exception {
        when(fileUtils.readFileCompletely(file)).thenReturn(fileSpans);
        when(delegate.sendSpans(fileSpans)).thenReturn(httpCall);
    }

    @Test
    void sendEmptyFile() throws Exception {
        Mockito.reset(fileUtils);
        Mockito.reset(delegate);
        File file = new File("/asdflkajsdfoij");
        when(fileUtils.readFileCompletely(file)).thenReturn(emptyList());
        FileSender sender = buildSender();
        boolean result = sender.handleFileOnDisk(file);
        assertFalse(result);
        verify(fileUtils).safeDelete(file);
    }

    @Test
    void happyPathSendSpans() {
        FileSender sender = buildSender();
        boolean result = sender.handleFileOnDisk(file);
        assertTrue(result);
        verify(bandwidthTracker).tick(fileSpans);
    }

    @Test
    void sendFailsButNotExceeded() throws Exception {
        when(httpCall.execute()).thenThrow(new IOException("boom"));
        FileSender sender = buildSender();
        boolean result = sender.handleFileOnDisk(file);
        assertFalse(result);
        verify(fileUtils, never()).safeDelete(any());
        verify(backoff).accept(1);
    }

    @Test
    void senderFailureRetriesExhausted() throws Exception {
        when(httpCall.execute()).thenThrow(new IOException("boom"));
        FileSender sender = buildSender(3);
        boolean result = sender.handleFileOnDisk(file);
        assertFalse(result);
        verify(fileUtils, never()).safeDelete(any());
        verify(backoff).accept(1);
        result = sender.handleFileOnDisk(file);
        assertFalse(result);
        verify(fileUtils, never()).safeDelete(any());
        verify(backoff).accept(2);
        result = sender.handleFileOnDisk(file);
        assertFalse(result);
        verify(fileUtils).safeDelete(file);
        verifyNoMoreInteractions(backoff);
    }

    @Test
    void readFileFails() throws IOException {
        Mockito.reset(fileUtils);
        Mockito.reset(delegate);
        when(fileUtils.readFileCompletely(file)).thenThrow(new IOException("boom"));
        FileSender sender = buildSender();
        boolean result = sender.handleFileOnDisk(file);
        assertFalse(result);
        verifyNoMoreInteractions(bandwidthTracker);
        verifyNoMoreInteractions(delegate);
    }

    private FileSender buildSender() {
        return buildSender(10);
    }

    private FileSender buildSender(int maxRetries) {
        return FileSender.builder()
                .backoff(backoff)
                .bandwidthTracker(bandwidthTracker)
                .maxRetries(maxRetries)
                .sender(delegate)
                .fileUtils(fileUtils)
                .build();
    }
}
