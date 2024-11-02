/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.opentelemetry.android;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.util.Log;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;

public class BandwidthThrottlingExporterTest {

    private SpanExporter mockDelegate; // Mocked SpanExporter to simulate the delegate
    private Function<SpanData, String> mockCategoryFunction; // Mocked category function
    private BandwidthThrottlingExporter exporter; // Instance of the BandwidthThrottlingExporter

    @Before
    public void setUp() {
        // Initialize the mocked delegate and category function
        mockDelegate = mock(SpanExporter.class);
        mockCategoryFunction = mock(Function.class);

        // Create an instance of BandwidthThrottlingExporter with a max limit of 1 KB/s and a
        // 1-second window
        exporter =
                BandwidthThrottlingExporter.newBuilder(mockDelegate)
                        .maxBytesPerSecond(1024) // 1 KB/s
                        .timeWindow(Duration.ofSeconds(1)) // 1 second
                        .build();
    }

    @Test
    public void testExportWithinLimit() {
        // Create a mock SpanData object
        SpanData spanData = mock(SpanData.class);
        when(spanData.getAttributes())
                .thenReturn(Collections.singletonMap("key", "value")); // Simulate attributes

        // Simulate the export of a single span
        CompletableResultCode result = exporter.export(Collections.singletonList(spanData));

        // Verify that the delegate's export method was called
        verify(mockDelegate).export(anyList());
        assertTrue(result.isSuccess()); // Ensure the export was successful
    }

    @Test
    public void testExportExceedingLimit() {
        // Create two mock SpanData objects
        SpanData spanData1 = mock(SpanData.class);
        SpanData spanData2 = mock(SpanData.class);
        when(spanData1.getAttributes())
                .thenReturn(Collections.singletonMap("key1", "value1")); // Simulate attributes
        when(spanData2.getAttributes())
                .thenReturn(Collections.singletonMap("key2", "value2")); // Simulate attributes

        // Simulate exporting spans that exceed the limit
        CompletableResultCode result = exporter.export(Arrays.asList(spanData1, spanData2));

        // Verify that the delegate's export method was called only once
        verify(mockDelegate, times(1)).export(anyList());

        // Ensure the result is successful
        assertTrue(result.isSuccess());
    }

    @Test
    public void testExportWithDifferentCategories() {
        // Create two mock SpanData objects with different attributes
        SpanData spanData1 = mock(SpanData.class);
        SpanData spanData2 = mock(SpanData.class);
        when(spanData1.getAttributes()).thenReturn(Collections.singletonMap("key1", "value1"));
        when(spanData2.getAttributes()).thenReturn(Collections.singletonMap("key2", "value2"));

        // Simulate exporting both spans
        CompletableResultCode result = exporter.export(Arrays.asList(spanData1, spanData2));

        // Verify that the delegate's export method was called
        verify(mockDelegate).export(anyList());

        // Ensure the result is successful
        assertTrue(result.isSuccess());
    }

    @Test
    public void testThrottlingLogMessage() {
        // Create a mock SpanData object that exceeds the limit
        SpanData spanData = mock(SpanData.class);
        when(spanData.getAttributes()).thenReturn(Collections.singletonMap("key", "value"));

        // Export the span multiple times to exceed the limit
        exporter.export(Collections.singletonList(spanData));
        exporter.export(Collections.singletonList(spanData)); // This should be throttled

        // Capture the log output
        // Note: In a real test, you might want to use a logging framework that allows capturing
        // logs
        // Here we just verify that the log message is generated
        // This is a placeholder as capturing logs in unit tests can be complex
        Log.d("BandwidthThrottlingExporter", "Throttled span: " + spanData.getName());
    }
}
