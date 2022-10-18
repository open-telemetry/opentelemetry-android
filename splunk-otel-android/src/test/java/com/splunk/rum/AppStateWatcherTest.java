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

import static java.util.Arrays.asList;
import static org.mockito.Mockito.inOrder;

import android.app.Activity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AppStateWatcherTest {

    @Mock Activity activity;
    @Mock AppStateListener listener1;
    @Mock AppStateListener listener2;

    AppStateWatcher underTest;

    @Before
    public void setUp() {
        underTest = new AppStateWatcher(asList(listener1, listener2));
    }

    @Test
    public void appForegrounded() {
        underTest.onActivityStarted(activity);
        underTest.onActivityStarted(activity);
        underTest.onActivityStopped(activity);
        underTest.onActivityStarted(activity);
        underTest.onActivityStopped(activity);

        InOrder io = inOrder(listener1, listener2);
        io.verify(listener1).appForegrounded();
        io.verify(listener2).appForegrounded();
        io.verifyNoMoreInteractions();
    }

    @Test
    public void appBackgrounded() {
        underTest.onActivityStarted(activity);
        underTest.onActivityStarted(activity);
        underTest.onActivityStopped(activity);
        underTest.onActivityStopped(activity);

        InOrder io = inOrder(listener1, listener2);
        io.verify(listener1).appForegrounded();
        io.verify(listener2).appForegrounded();
        io.verify(listener1).appBackgrounded();
        io.verify(listener2).appBackgrounded();
        io.verifyNoMoreInteractions();
    }
}
