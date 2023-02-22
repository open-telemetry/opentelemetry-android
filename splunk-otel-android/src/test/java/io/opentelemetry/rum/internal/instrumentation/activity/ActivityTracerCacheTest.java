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

package io.opentelemetry.rum.internal.instrumentation.activity;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.Activity;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityTracerCacheTest {

    @Mock Activity activity;

    @Mock ActivityTracer activityTracer;
    @Mock Function<Activity, ActivityTracer> tracerCreator;
    AtomicReference<String> initialActivity;

    @BeforeEach
    void setup() {
        initialActivity = new AtomicReference<>();
    }

    @Test
    void addEventNewActivity() {
        when(tracerCreator.apply(activity)).thenReturn(activityTracer);
        when(activityTracer.addEvent(anyString())).thenReturn(activityTracer);

        ActivityTracerCache underTest = new ActivityTracerCache(tracerCreator);
        ActivityTracer result = underTest.addEvent(activity, "beep");
        assertSame(activityTracer, result);
        verify(activityTracer).addEvent("beep");
        verifyNoMoreInteractions(tracerCreator);
    }

    @Test
    void addEventExistingActivity() {
        when(tracerCreator.apply(activity)).thenReturn(activityTracer);
        when(activityTracer.addEvent(anyString())).thenReturn(activityTracer);

        ActivityTracerCache underTest = new ActivityTracerCache(tracerCreator);
        ActivityTracer result1 = underTest.addEvent(activity, "beep1");
        ActivityTracer result2 = underTest.addEvent(activity, "beep2");
        ActivityTracer result3 = underTest.addEvent(activity, "beep3");
        assertSame(activityTracer, result1);
        assertSame(activityTracer, result2);
        assertSame(activityTracer, result3);
        verify(activityTracer).addEvent("beep1");
        verify(activityTracer).addEvent("beep2");
        verify(activityTracer).addEvent("beep3");
        verify(tracerCreator).apply(activity);
    }

    @Test
    void startSpanIfNoneInProgress() {
        when(tracerCreator.apply(activity)).thenReturn(activityTracer);
        when(activityTracer.startSpanIfNoneInProgress("wrenchy")).thenReturn(activityTracer);

        ActivityTracerCache underTest = new ActivityTracerCache(tracerCreator);

        ActivityTracer result = underTest.startSpanIfNoneInProgress(activity, "wrenchy");
        assertSame(activityTracer, result);
        verify(activityTracer).startSpanIfNoneInProgress("wrenchy");
        verifyNoMoreInteractions(tracerCreator);
    }

    @Test
    void initiateRestartSpanIfNecessary_singleActivity() {

        when(tracerCreator.apply(activity)).thenReturn(activityTracer);
        when(activityTracer.initiateRestartSpanIfNecessary(false)).thenReturn(activityTracer);

        ActivityTracerCache underTest = new ActivityTracerCache(tracerCreator);

        ActivityTracer result = underTest.initiateRestartSpanIfNecessary(activity);
        assertSame(activityTracer, result);
        verify(activityTracer).initiateRestartSpanIfNecessary(false);
        verifyNoMoreInteractions(tracerCreator);
    }

    @Test
    void initiateRestartSpanIfNecessary_multiActivity() {
        Activity activity2 = new Activity() {
                    // to get a new class name used in the cache
                };
        ActivityTracer activityTracer2 = mock(ActivityTracer.class);

        when(tracerCreator.apply(activity)).thenReturn(activityTracer);
        when(tracerCreator.apply(activity2)).thenReturn(activityTracer2);
        when(activityTracer.addEvent(anyString())).thenReturn(activityTracer);
        when(activityTracer.initiateRestartSpanIfNecessary(true)).thenReturn(activityTracer);

        ActivityTracerCache underTest = new ActivityTracerCache(tracerCreator);

        underTest.addEvent(activity, "foo");
        underTest.addEvent(activity2, "bar");
        ActivityTracer result = underTest.initiateRestartSpanIfNecessary(activity);
        assertSame(activityTracer, result);
        verify(activityTracer).initiateRestartSpanIfNecessary(true);
    }

    @Test
    void startActivityCreation() {
        when(tracerCreator.apply(activity)).thenReturn(activityTracer);
        when(activityTracer.startActivityCreation()).thenReturn(activityTracer);

        ActivityTracerCache underTest = new ActivityTracerCache(tracerCreator);

        ActivityTracer result = underTest.startActivityCreation(activity);
        assertSame(activityTracer, result);
        verify(activityTracer).startActivityCreation();
    }
}
