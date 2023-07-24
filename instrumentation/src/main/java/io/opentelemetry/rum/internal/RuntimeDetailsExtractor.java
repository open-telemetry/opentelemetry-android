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

package io.opentelemetry.rum.internal;

import static io.opentelemetry.rum.internal.RumConstants.BATTERY_PERCENT_KEY;
import static io.opentelemetry.rum.internal.RumConstants.HEAP_FREE_KEY;
import static io.opentelemetry.rum.internal.RumConstants.STORAGE_SPACE_FREE_KEY;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import androidx.annotation.Nullable;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.io.File;

/** Represents details about the runtime environment at a time */
public final class RuntimeDetailsExtractor<RQ, RS> extends BroadcastReceiver
        implements AttributesExtractor<RQ, RS> {

    private @Nullable volatile Double batteryPercent = null;
    private final File filesDir;

    public static <RQ, RS> RuntimeDetailsExtractor<RQ, RS> create(Context context) {
        IntentFilter batteryChangedFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        File filesDir = context.getFilesDir();
        RuntimeDetailsExtractor<RQ, RS> runtimeDetails = new RuntimeDetailsExtractor<>(filesDir);
        context.registerReceiver(runtimeDetails, batteryChangedFilter);
        return runtimeDetails;
    }

    private RuntimeDetailsExtractor(File filesDir) {
        this.filesDir = filesDir;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        batteryPercent = level * 100.0d / (float) scale;
    }

    @Override
    public void onStart(
            AttributesBuilder attributes,
            io.opentelemetry.context.Context parentContext,
            RQ request) {
        attributes.put(STORAGE_SPACE_FREE_KEY, getCurrentStorageFreeSpaceInBytes());
        attributes.put(HEAP_FREE_KEY, getCurrentFreeHeapInBytes());

        Double currentBatteryPercent = getCurrentBatteryPercent();
        if (currentBatteryPercent != null) {
            attributes.put(BATTERY_PERCENT_KEY, currentBatteryPercent);
        }
    }

    @Override
    public void onEnd(
            AttributesBuilder attributes,
            io.opentelemetry.context.Context context,
            RQ request,
            RS response,
            Throwable error) {}

    private long getCurrentStorageFreeSpaceInBytes() {
        return filesDir.getFreeSpace();
    }

    private long getCurrentFreeHeapInBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.freeMemory();
    }

    @Nullable
    private Double getCurrentBatteryPercent() {
        return batteryPercent;
    }
}
