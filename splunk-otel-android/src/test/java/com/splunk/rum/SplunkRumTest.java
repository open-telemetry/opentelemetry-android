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

import android.app.Application;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SplunkRumTest {

    @Before
    public void setUp() {
        SplunkRum.resetSingletonForTest();
    }

    @Test
    public void initialization_onlyOnce() {
        Config config = mock(Config.class);
        when(config.getBeaconUrl()).thenReturn("http://backend");
        when(config.isDebugEnabled()).thenReturn(true);
        SplunkRum singleton = SplunkRum.initialize(config, mock(Application.class));
        SplunkRum sameInstance = SplunkRum.initialize(config, mock(Application.class));

        assertSame(singleton, sameInstance);
    }

    @Test
    public void getInstance_preConfig() {
        assertThrows(IllegalStateException.class, SplunkRum::getInstance);
    }

    @Test
    public void getInstance() {
        Config config = mock(Config.class);
        when(config.getBeaconUrl()).thenReturn("http://backend");
        SplunkRum singleton = SplunkRum.initialize(config, mock(Application.class));
        assertSame(singleton, SplunkRum.getInstance());
    }

    @Test
    public void newConfigBuilder() {
        assertNotNull(SplunkRum.newConfigBuilder());
    }
}