package com.splunk.rum;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ZipkinToDiskSenderTest {

    private final long now = System.currentTimeMillis();
    private final File path = new File("/my/great/storage/location");
    private final String finalFile = "/my/great/storage/location/" + now + ".spans";
    private final File finalPath = new File(finalFile);
    private final byte[] span1 = "span one".getBytes(StandardCharsets.UTF_8);
    private final byte[] span2 = "span one".getBytes(StandardCharsets.UTF_8);
    private final List<byte[]> spans = Arrays.asList(span1, span2);

    @Mock
    private FileUtils fileUtils;
    @Mock
    private Clock clock;
    @Mock
    private DeviceSpanStorageLimiter limiter;

    @Before
    public void setup() {
        when(clock.millis()).thenReturn(now);
        when(limiter.ensureFreeSpace()).thenReturn(true);
    }

    @Test
    public void testHappyPath() throws Exception {

        ZipkinToDiskSender sender = ZipkinToDiskSender.builder()
                .path(path)
                .fileUtils(fileUtils)
                .clock(clock)
                .storageLimiter(limiter)
                .build();
        sender.sendSpans(spans);

        verify(fileUtils).writeAsLines(finalPath, spans);
    }

    @Test
    public void testWriteFails() throws Exception {
        doThrow(new IOException("boom")).when(fileUtils).writeAsLines(finalPath, spans);

        ZipkinToDiskSender sender = ZipkinToDiskSender.builder()
                .path(path)
                .fileUtils(fileUtils)
                .clock(clock)
                .storageLimiter(limiter)
                .build();

        sender.sendSpans(spans);
        // Exception not thrown
    }

    @Test
    public void testLimitExceeded() throws Exception {

        when(limiter.ensureFreeSpace()).thenReturn(false);

        ZipkinToDiskSender sender = ZipkinToDiskSender.builder()
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