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

import androidx.annotation.NonNull;

class ConfigFlags {
    private boolean debugEnabled = false;
    private boolean diskBufferingEnabled = false;
    private boolean reactNativeSupportEnabled = false;
    private boolean crashReportingEnabled = true;
    private boolean networkMonitorEnabled = true;
    private boolean anrDetectionEnabled = true;
    private boolean slowRenderingDetectionEnabled = true;

    void enableDebug() {
        debugEnabled = true;
    }

    void enableDiskBuffering() {
        diskBufferingEnabled = true;
    }

    void enableReactNativeSupport() {
        reactNativeSupportEnabled = true;
    }

    void disableCrashReporting() {
        crashReportingEnabled = false;
    }

    void disableNetworkMonitor() {
        networkMonitorEnabled = false;
    }

    void disableAnrDetection() {
        anrDetectionEnabled = false;
    }

    void disableSlowRenderingDetection() {
        slowRenderingDetectionEnabled = false;
    }

    boolean isDebugEnabled() {
        return debugEnabled;
    }

    boolean isAnrDetectionEnabled() {
        return anrDetectionEnabled;
    }

    boolean isNetworkMonitorEnabled() {
        return networkMonitorEnabled;
    }

    boolean isSlowRenderingDetectionEnabled() {
        return slowRenderingDetectionEnabled;
    }

    boolean isCrashReportingEnabled() {
        return crashReportingEnabled;
    }

    boolean isDiskBufferingEnabled() {
        return diskBufferingEnabled;
    }

    boolean isReactNativeSupportEnabled() {
        return reactNativeSupportEnabled;
    }

    @NonNull
    @Override
    public String toString() {
        return "[debug:"
                + debugEnabled
                + ","
                + "crashReporting:"
                + crashReportingEnabled
                + ","
                + "anrReporting:"
                + anrDetectionEnabled
                + ","
                + "slowRenderingDetector:"
                + slowRenderingDetectionEnabled
                + ","
                + "networkMonitor:"
                + networkMonitorEnabled
                + "]";
    }
}
