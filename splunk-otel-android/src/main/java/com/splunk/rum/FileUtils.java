package com.splunk.rum;

import static com.splunk.rum.SplunkRum.LOG_TAG;

import android.app.Application;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;
import android.util.AtomicFile;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.stream.Stream;

// Basic wrapper around filesystem operations, primarily for testing
class FileUtils {

    static File getSpansDirectory(Application application) {
        File filesDir = application.getApplicationContext().getFilesDir();
        return new File(filesDir, "spans");
    }

    void writeAsLines(File file, List<byte[]> blocksOfData) throws IOException {
        AtomicFile outfile = new AtomicFile(file);
        try (FileOutputStream out = outfile.startWrite()) {
            for (byte[] encodedSpan : blocksOfData) {
                out.write(encodedSpan);
                out.write('\n');
            }
            outfile.finishWrite(out);
        }
    }

    List<byte[]> readFileCompletely(File file) throws IOException {
        List<byte[]> result = new ArrayList<>();
        try (FileReader fileReader = new FileReader(file)) {
            try (BufferedReader buff = new BufferedReader(fileReader)) {
                String line;
                while ((line = buff.readLine()) != null) {
                    result.add(line.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        return result;
    }

    Stream<File> listFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return Stream.empty();
        }
        return Arrays.stream(files);
    }

    Stream<File> listSpanFiles(File dir) {
        return listFiles(dir)
                .filter(this::isRegularFile)
                .filter(file -> file.toString().endsWith(".spans"));
    }

    long getTotalFileSizeInBytes(File dir) {
        return listFiles(dir)
                .reduce(0L, (acc, file) -> acc + getFileSize(file), Long::sum);
    }

    long getFileSize(File file) {
        try {
            StructStat structStat = Os.stat(file.getCanonicalPath());
            return structStat.st_size;
        } catch (ErrnoException | IOException e) {
            Log.w(LOG_TAG, "Error getting file size for " + file, e);
            return 0L;
        }
    }

    long getModificationTime(File file) {
        try {
            StructStat structStat = Os.stat(file.getCanonicalPath());
            return structStat.st_mtime;
        } catch (ErrnoException | IOException e) {
            Log.w(LOG_TAG, "Error getting file size for " + file, e);
            return 0L;
        }
    }

    boolean isRegularFile(File file) {
        return file.isFile();
    }

    void safeDelete(File file) {
        if (!file.delete()) {
            Log.w(LOG_TAG, "Error deleting file " + file);
        }
    }
}
