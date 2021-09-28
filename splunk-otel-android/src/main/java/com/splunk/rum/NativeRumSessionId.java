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

import android.webkit.JavascriptInterface;

/**
 * Object to inject into WebViews as a javascript object, in order to integrate with browser RUM.
 */
class NativeRumSessionId {
    private final SplunkRum splunkRum;

    public NativeRumSessionId(SplunkRum splunkRum) {
        this.splunkRum = splunkRum;
    }

    @JavascriptInterface
    public String getNativeSessionId() {
        return splunkRum.getRumSessionId();
    }
}
