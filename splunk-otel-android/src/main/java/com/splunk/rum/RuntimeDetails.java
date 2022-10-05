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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import androidx.annotation.Nullable;
import java.io.File;

/** Represents details about the runtime environment at a time */
final class RuntimeDetails extends BroadcastReceiver {

    private @Nullable volatile Double batteryPercent = null;
    private final File filesDir;

    static RuntimeDetails create(Context context) {
        IntentFilter batteryChangedFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        File filesDir = context.getFilesDir();
        RuntimeDetails runtimeDetails = new RuntimeDetails(filesDir);
        context.registerReceiver(runtimeDetails, batteryChangedFilter);
        return runtimeDetails;
    }

    private RuntimeDetails(File filesDir) {
        this.filesDir = filesDir;
    }

    long getCurrentStorageFreeSpaceInBytes() {
        return filesDir.getFreeSpace();
    }

    long getCurrentFreeHeapInBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.freeMemory();
    }

    @Nullable
    Double getCurrentBatteryPercent() {
        return batteryPercent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        batteryPercent = level * 100.0d / (float) scale;
    }
}
