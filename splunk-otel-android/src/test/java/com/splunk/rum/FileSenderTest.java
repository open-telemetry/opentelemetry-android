package com.splunk.rum;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static java.util.Collections.emptyList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import zipkin2.Call;
import zipkin2.reporter.Sender;

@RunWith(MockitoJUnitRunner.class)
public class FileSenderTest {

    private final byte[] span1 = "span1".getBytes(StandardCharsets.UTF_8);
    private final byte[] span2 = "span2".getBytes(StandardCharsets.UTF_8);
    private final byte[] span3 = "span3".getBytes(StandardCharsets.UTF_8);
    private final File file = new File("meep");
    private final List<byte[]> fileSpans = Arrays.asList(span1, span2, span3);

    @Mock
    private FileUtils fileUtils;
    @Mock
    private BandwidthTracker bandwidthTracker;
    @Mock
    private Sender delegate;
    @Mock
    private Call<Void> httpCall;
    @Mock
    private Consumer<Integer> backoff;

    @Before
    public void setup() throws Exception {
        when(fileUtils.readFileCompletely(file)).thenReturn(fileSpans);
        when(delegate.sendSpans(fileSpans)).thenReturn(httpCall);
    }

    @Test
    public void testEmptyFile() throws Exception {
        File file = new File("/asdflkajsdfoij");
        when(fileUtils.readFileCompletely(file)).thenReturn(emptyList());
        FileSender sender = buildSender();
        boolean result = sender.handleFileOnDisk(file);
        assertFalse(result);
    }

    @Test
    public void testHappyPathSendSpans() {
        FileSender sender = buildSender();
        boolean result = sender.handleFileOnDisk(file);
        assertTrue(result);
        verify(bandwidthTracker).tick(fileSpans);
    }

    @Test
    public void testSendFailsButNotExceeded() throws Exception {
        when(httpCall.execute()).thenThrow(new IOException("boom"));
        FileSender sender = buildSender();
        boolean result = sender.handleFileOnDisk(file);
        assertFalse(result);
        verify(fileUtils, never()).safeDelete(any());
        verify(backoff).accept(1);
    }

    @Test
    public void testSenderFailureRetriesExhausted() throws Exception {
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
    public void testReadFileFails() throws IOException {
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