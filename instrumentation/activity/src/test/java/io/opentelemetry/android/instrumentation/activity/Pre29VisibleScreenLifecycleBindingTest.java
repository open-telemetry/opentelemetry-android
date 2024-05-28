/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity;

import static org.mockito.Mockito.verify;

import android.app.Activity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Pre29VisibleScreenLifecycleBindingTest {
    @Mock Activity activity;
    @Mock VisibleScreenTracker tracker;

    @Test
    void postResumed() {
        Pre29VisibleScreenLifecycleBinding underTest =
                new Pre29VisibleScreenLifecycleBinding(tracker);
        underTest.onActivityResumed(activity);
        verify(tracker).activityResumed(activity);
        Mockito.verifyNoMoreInteractions(tracker);
    }

    @Test
    void prePaused() {
        Pre29VisibleScreenLifecycleBinding underTest =
                new Pre29VisibleScreenLifecycleBinding(tracker);
        underTest.onActivityPaused(activity);
        verify(tracker).activityPaused(activity);
        Mockito.verifyNoMoreInteractions(tracker);
    }
}
