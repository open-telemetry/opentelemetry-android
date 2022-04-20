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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class MemoryBufferingExporterTest {
    private final ConnectionUtil connectionUtil = mock(ConnectionUtil.class);

    @Before
    public void setUp() {
        when(connectionUtil.refreshNetworkStatus())
                .thenReturn(new CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, null));
    }

    @Test
    public void happyPath() {
        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(connectionUtil, delegate);

        Collection<SpanData> spans = Arrays.asList(mock(SpanData.class), mock(SpanData.class));
        when(delegate.export(spans)).thenReturn(CompletableResultCode.ofSuccess());

        CompletableResultCode result = bufferingExporter.export(spans);
        assertTrue(result.isSuccess());
    }

    @Test
    public void offlinePath() {
        when(connectionUtil.refreshNetworkStatus())
                .thenReturn(new CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE, null))
                .thenReturn(new CurrentNetwork(NetworkState.TRANSPORT_UNKNOWN, null));

        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(connectionUtil, delegate);

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
    public void retryPath() {
        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(connectionUtil, delegate);

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
    public void flush_withBacklog() {
        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(connectionUtil, delegate);

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
    public void flush() {
        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(connectionUtil, delegate);
        when(delegate.flush()).thenReturn(CompletableResultCode.ofSuccess());

        CompletableResultCode secondResult = bufferingExporter.flush();
        assertTrue(secondResult.isSuccess());
        verify(delegate).flush();
    }

    @Test
    public void maxBacklog() {
        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(connectionUtil, delegate);

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
    public void shutdown() {
        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(connectionUtil, delegate);

        bufferingExporter.shutdown();
        verify(delegate).shutdown();
    }
}
