package com.splunk.rum;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static java.util.Collections.singletonList;

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
import java.util.stream.Stream;

import zipkin2.Call;
import zipkin2.reporter.Sender;

@RunWith(MockitoJUnitRunner.class)
public class DiskToZipkinExporterTest {

    static final int BANDWIDTH_LIMIT = 20 * 1024;
    static final File spanFilesPath = new File("/path/to/thing");
    private final byte[] span1 = "span1".getBytes(StandardCharsets.UTF_8);
    private final byte[] span2 = "span2".getBytes(StandardCharsets.UTF_8);
    private final byte[] span3 = "span3".getBytes(StandardCharsets.UTF_8);
    private final File file1 = new File(spanFilesPath.getAbsolutePath() + File.separator + "file1.spans");
    private final File file2 = new File(spanFilesPath.getAbsolutePath() + File.separator + "file2.spans");
    private final List<byte[]> file1Spans = singletonList(span1);
    private final List<byte[]> file2Spans = Arrays.asList(span2, span3);
    private final File imposter = new File(spanFilesPath.getAbsolutePath() + File.separator + "someImposterFile.dll");

    @Mock
    private ConnectionUtil connectionUtil;
    @Mock
    private FileUtils fileUtils;
    @Mock
    private CurrentNetwork currentNetwork;
    @Mock
    Sender sender;
    @Mock
    private BandwidthTracker bandwidthTracker;

    @Before
    public void setup() throws Exception{
        when(connectionUtil.refreshNetworkStatus()).thenReturn(currentNetwork);
        when(currentNetwork.isOnline()).thenReturn(true);
        Stream<File> files = Stream.of(file1, imposter, file2);
        when(fileUtils.listFiles(spanFilesPath)).thenReturn(files);
        when(fileUtils.isRegularFile(file1)).thenReturn(true);
        when(fileUtils.isRegularFile(file2)).thenReturn(true);
        when(fileUtils.isRegularFile(imposter)).thenReturn(true);
        when(fileUtils.readFileCompletely(file1)).thenReturn(file1Spans);
        when(fileUtils.readFileCompletely(file2)).thenReturn(file2Spans);
    }

    @Test
    public void testHappyPathExport() throws Exception {

        Call<Void> call1 = mock(Call.class);
        Call<Void> call2 = mock(Call.class);

        when(sender.sendSpans(file1Spans)).thenReturn(call1);
        when(sender.sendSpans(file2Spans)).thenReturn(call2);

        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();
        verify(call1).execute();
        verify(call2).execute();
        verify(fileUtils).safeDelete(file1);
        verify(fileUtils).safeDelete(file2);
        verify(fileUtils, never()).readFileCompletely(imposter);
        verify(bandwidthTracker).tick(file1Spans);
        verify(bandwidthTracker).tick(file2Spans);
    }

    @Test
    public void fileFailureSkipsSubsequentFiles() throws Exception {

        when(fileUtils.readFileCompletely(file1)).thenThrow(new IOException("no file"));

        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();

        verify(fileUtils, never()).readFileCompletely(file2);
        verifyNoMoreInteractions(sender);
    }

    @Test
    public void testSkipsWhenOffline() {
        when(currentNetwork.isOnline()).thenReturn(false);

        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();

        verifyNoMoreInteractions(fileUtils);
        verifyNoMoreInteractions(sender);
    }

    @Test
    public void testSkipsWhenOverBandwidth() throws Exception {
        when(bandwidthTracker.totalSustainedRate()).thenReturn(BANDWIDTH_LIMIT+1.0);

        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();

        verify(fileUtils, never()).readFileCompletely(any());
        verifyNoMoreInteractions(sender);
    }

    @Test
    public void testOtherExceptionsHandled() throws Exception {
        when(fileUtils.listFiles(spanFilesPath)).thenThrow(new RuntimeException("unexpected!"));
        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();
        verify(fileUtils, never()).readFileCompletely(any());
        verify(fileUtils, never()).safeDelete(any());
        verifyNoMoreInteractions(sender);
    }

    @Test
    public void testSenderFailure() throws Exception {
        Call<Void> call1 = mock(Call.class);
        Call<Void> call2 = mock(Call.class);

        when(sender.sendSpans(file1Spans)).thenReturn(call1);
        when(call1.execute()).thenThrow(new IOException("Failure is yours to enjoy"));

        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();
        verify(fileUtils).safeDelete(file1);
        verify(fileUtils, never()).readFileCompletely(file2);
        verify(fileUtils, never()).safeDelete(file2);
    }

    private DiskToZipkinExporter buildExporter() {
        return DiskToZipkinExporter.builder()
                .fileUtils(fileUtils)
                .sender(sender)
                .bandwidthLimit(BANDWIDTH_LIMIT)
                .bandwidthTracker(bandwidthTracker)
                .spanFilesPath(spanFilesPath)
                .connectionUtil(connectionUtil)
                .build();
    }

}