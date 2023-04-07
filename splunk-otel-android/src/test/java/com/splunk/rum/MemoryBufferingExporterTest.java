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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.rum.internal.instrumentation.network.CurrentNetwork;
import io.opentelemetry.rum.internal.instrumentation.network.CurrentNetworkProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class MemoryBufferingExporterTest {
    private final CurrentNetworkProvider currentNetworkProvider =
            mock(CurrentNetworkProvider.class);
    private final CurrentNetwork currentNetwork = mock(CurrentNetwork.class);

    @BeforeEach
    void setUp() {
        when(currentNetworkProvider.refreshNetworkStatus()).thenReturn(currentNetwork);
    }

    @Test
    void happyPath() {
        when(currentNetwork.isOnline()).thenReturn(true);

        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(currentNetworkProvider, delegate);

        Collection<SpanData> spans = Arrays.asList(mock(SpanData.class), mock(SpanData.class));
        when(delegate.export(spans)).thenReturn(CompletableResultCode.ofSuccess());

        CompletableResultCode result = bufferingExporter.export(spans);
        assertTrue(result.isSuccess());
    }

    @Test
    void offlinePath() {
        when(currentNetwork.isOnline()).thenReturn(false, true);

        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(currentNetworkProvider, delegate);

        Collection<SpanData> spans = Arrays.asList(mock(SpanData.class), mock(SpanData.class));

        CompletableResultCode result = bufferingExporter.export(spans);
        assertTrue(result.isSuccess());
        verify(delegate, never()).export(any());

        List<SpanData> secondBatch = new ArrayList<>(spans);
        SpanData anotherSpan = mock(SpanData.class);
        secondBatch.add(anotherSpan);
        when(delegate.export(secondBatch)).thenReturn(CompletableResultCode.ofSuccess());

        // send another span now that we're back online.
        result = bufferingExporter.export(Collections.singleton(anotherSpan));

        assertTrue(result.isSuccess());
        verify(delegate).export(secondBatch);
    }

    @Test
    void retryPath() {
        when(currentNetwork.isOnline()).thenReturn(true);

        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(currentNetworkProvider, delegate);

        SpanData one = mock(SpanData.class);
        SpanData two = mock(SpanData.class);
        SpanData three = mock(SpanData.class);
        Collection<SpanData> spans = Arrays.asList(one, two);
        when(delegate.export(spans)).thenReturn(CompletableResultCode.ofFailure());
        when(delegate.export(Arrays.asList(one, two, three)))
                .thenReturn(CompletableResultCode.ofSuccess());

        CompletableResultCode firstResult = bufferingExporter.export(spans);
        assertFalse(firstResult.isSuccess());

        CompletableResultCode secondResult =
                bufferingExporter.export(Collections.singletonList(three));
        assertTrue(secondResult.isSuccess());
    }

    @Test
    void flush_withBacklog() {
        when(currentNetwork.isOnline()).thenReturn(true);

        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(currentNetworkProvider, delegate);

        SpanData one = mock(SpanData.class);
        SpanData two = mock(SpanData.class);
        Collection<SpanData> spans = Arrays.asList(one, two);
        when(delegate.export(spans))
                .thenReturn(CompletableResultCode.ofFailure())
                .thenReturn(CompletableResultCode.ofSuccess());

        CompletableResultCode firstResult = bufferingExporter.export(spans);
        assertFalse(firstResult.isSuccess());

        CompletableResultCode secondResult = bufferingExporter.flush();
        assertTrue(secondResult.isSuccess());
        // 2 times...once from the failure, and once from the flush with success
        verify(delegate, times(2)).export(spans);
    }

    @Test
    void flush() {
        when(currentNetwork.isOnline()).thenReturn(true);

        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(currentNetworkProvider, delegate);
        when(delegate.flush()).thenReturn(CompletableResultCode.ofSuccess());

        CompletableResultCode secondResult = bufferingExporter.flush();
        assertTrue(secondResult.isSuccess());
        verify(delegate).flush();
    }

    @SuppressWarnings("unchecked")
    @Test
    void maxBacklog() {
        when(currentNetwork.isOnline()).thenReturn(true);

        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(currentNetworkProvider, delegate);

        List<SpanData> firstSet = new ArrayList<>();
        for (int i = 0; i < 110; i++) {
            firstSet.add(mock(SpanData.class));
        }
        when(delegate.export(firstSet)).thenReturn(CompletableResultCode.ofFailure());

        CompletableResultCode firstResult = bufferingExporter.export(firstSet);
        assertFalse(firstResult.isSuccess());

        List<SpanData> secondSet = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            secondSet.add(mock(SpanData.class));
        }

        ArgumentCaptor<List<SpanData>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        when(delegate.export(argumentCaptor.capture()))
                .thenReturn(CompletableResultCode.ofSuccess());

        CompletableResultCode secondResult = bufferingExporter.export(secondSet);
        assertTrue(secondResult.isSuccess());

        List<SpanData> value = argumentCaptor.getValue();
        // we keep only 100 of the first 110 that failed.
        assertEquals(120, value.size());
    }

    @Test
    void shutdown() {
        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(currentNetworkProvider, delegate);

        bufferingExporter.shutdown();
        verify(delegate).shutdown();
    }
}
