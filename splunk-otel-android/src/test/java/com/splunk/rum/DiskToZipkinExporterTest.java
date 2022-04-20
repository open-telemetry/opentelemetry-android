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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DiskToZipkinExporterTest {

    static final int BANDWIDTH_LIMIT = 20 * 1024;
    static final File spanFilesPath = new File("/path/to/thing");
    private final File file1 =
            new File(spanFilesPath.getAbsolutePath() + File.separator + "file1.spans");
    private final File file2 =
            new File(spanFilesPath.getAbsolutePath() + File.separator + "file2.spans");
    private final File imposter =
            new File(spanFilesPath.getAbsolutePath() + File.separator + "someImposterFile.dll");

    @Mock private ConnectionUtil connectionUtil;
    @Mock private FileUtils fileUtils;
    @Mock private CurrentNetwork currentNetwork;
    @Mock FileSender sender;
    @Mock private BandwidthTracker bandwidthTracker;

    @Before
    public void setup() throws Exception {
        when(connectionUtil.refreshNetworkStatus()).thenReturn(currentNetwork);
        when(currentNetwork.isOnline()).thenReturn(true);
        Stream<File> files = Stream.of(file1, imposter, file2);
        when(fileUtils.listSpanFiles(spanFilesPath)).thenReturn(files);
    }

    @Test
    public void testHappyPathExport() throws Exception {
        when(sender.handleFileOnDisk(file1)).thenReturn(true);
        when(sender.handleFileOnDisk(file2)).thenReturn(true);

        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();
        verify(sender).handleFileOnDisk(file1);
        verify(sender).handleFileOnDisk(file2);
        verify(bandwidthTracker, never()).tick(anyList());
    }

    @Test
    public void fileFailureSkipsSubsequentFiles() throws Exception {

        when(sender.handleFileOnDisk(file1)).thenReturn(false);

        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();

        verify(sender).handleFileOnDisk(file1);
        verify(sender, never()).handleFileOnDisk(file2);
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
        when(bandwidthTracker.totalSustainedRate()).thenReturn(BANDWIDTH_LIMIT + 1.0);

        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();

        verify(sender, never()).handleFileOnDisk(any());
    }

    @Test
    public void testOtherExceptionsHandled() throws Exception {
        when(fileUtils.listSpanFiles(spanFilesPath)).thenThrow(new RuntimeException("unexpected!"));
        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();
        verify(sender, never()).handleFileOnDisk(any());
    }

    private DiskToZipkinExporter buildExporter() {
        return DiskToZipkinExporter.builder()
                .fileUtils(fileUtils)
                .fileSender(sender)
                .bandwidthLimit(BANDWIDTH_LIMIT)
                .bandwidthTracker(bandwidthTracker)
                .spanFilesPath(spanFilesPath)
                .connectionUtil(connectionUtil)
                .build();
    }
}
