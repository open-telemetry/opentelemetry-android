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

import static org.mockito.Mockito.inOrder;

import android.app.Activity;

import io.opentelemetry.rum.internal.instrumentation.ApplicationStateListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationStateWatcherTest {

    @Mock Activity activity;
    @Mock ApplicationStateListener listener1;
    @Mock ApplicationStateListener listener2;

    ApplicationStateWatcher underTest;

    @BeforeEach
    void setUp() {
        underTest = new ApplicationStateWatcher();
        underTest.registerListener(listener1);
        underTest.registerListener(listener2);
    }

    @Test
    void appForegrounded() {
        underTest.onActivityStarted(activity);
        underTest.onActivityStarted(activity);
        underTest.onActivityStopped(activity);
        underTest.onActivityStarted(activity);
        underTest.onActivityStopped(activity);

        InOrder io = inOrder(listener1, listener2);
        io.verify(listener1).onApplicationForegrounded();
        io.verify(listener2).onApplicationForegrounded();
        io.verifyNoMoreInteractions();
    }

    @Test
    void appBackgrounded() {
        underTest.onActivityStarted(activity);
        underTest.onActivityStarted(activity);
        underTest.onActivityStopped(activity);
        underTest.onActivityStopped(activity);

        InOrder io = inOrder(listener1, listener2);
        io.verify(listener1).onApplicationForegrounded();
        io.verify(listener2).onApplicationForegrounded();
        io.verify(listener1).onApplicationBackgrounded();
        io.verify(listener2).onApplicationBackgrounded();
        io.verifyNoMoreInteractions();
    }
}
