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

// Includes work from:
/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import androidx.annotation.Nullable;

/**
 * Copy of
 * https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/710429029450855c051ce45c8a8857c95f1cb255/instrumentation/reactor/reactor-netty/reactor-netty-1.0/javaagent/src/main/java/io/opentelemetry/javaagent/instrumentation/reactornetty/v1_0/UrlParser.java
 */
class UrlParser {

    @Nullable
    static String getHost(String url) {

        int schemeEndIndex = url.indexOf(':');
        if (schemeEndIndex == -1) {
            // not a valid url
            return null;
        }

        int len = url.length();
        if (len <= schemeEndIndex + 2
                || url.charAt(schemeEndIndex + 1) != '/'
                || url.charAt(schemeEndIndex + 2) != '/') {
            // has no authority component
            return null;
        }

        // look for the end of the host:
        //   ':' ==> start of port, or
        //   '/', '?', '#' ==> start of path
        int index;
        for (index = schemeEndIndex + 3; index < len; index++) {
            char c = url.charAt(index);
            if (c == ':' || c == '/' || c == '?' || c == '#') {
                break;
            }
        }
        String host = url.substring(schemeEndIndex + 3, index);
        return host.isEmpty() ? null : host;
    }

    @Nullable
    static Integer getPort(String url) {

        int schemeEndIndex = url.indexOf(':');
        if (schemeEndIndex == -1) {
            // not a valid url
            return null;
        }

        int len = url.length();
        if (len <= schemeEndIndex + 2
                || url.charAt(schemeEndIndex + 1) != '/'
                || url.charAt(schemeEndIndex + 2) != '/') {
            // has no authority component
            return null;
        }

        // look for the end of the host:
        //   ':' ==> start of port, or
        //   '/', '?', '#' ==> start of path
        int index;
        int portIndex = -1;
        for (index = schemeEndIndex + 3; index < len; index++) {
            char c = url.charAt(index);
            if (c == ':') {
                portIndex = index + 1;
                break;
            }
            if (c == '/' || c == '?' || c == '#') {
                break;
            }
        }

        if (portIndex == -1) {
            return null;
        }

        // look for the end of the port:
        //   '/', '?', '#' ==> start of path
        for (index = portIndex; index < len; index++) {
            char c = url.charAt(index);
            if (c == '/' || c == '?' || c == '#') {
                break;
            }
        }
        String port = url.substring(portIndex, index);
        return port.isEmpty() ? null : safeParse(port);
    }

    @Nullable
    private static Integer safeParse(String port) {
        try {
            return Integer.valueOf(port);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private UrlParser() {}
}
