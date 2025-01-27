/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.activities;

import static org.mockito.Mockito.verify;

import android.app.Activity;
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Pre29VisibleScreenLifecycleBindingTest {
    @Mock Activity activity;
    @Mock VisibleScreenTracker visibleScreenTracker;

    @Test
    void postResumed() {
        Pre29VisibleScreenLifecycleBinding underTest =
                new Pre29VisibleScreenLifecycleBinding(visibleScreenTracker);
        underTest.onActivityResumed(activity);
        verify(visibleScreenTracker).activityResumed(activity);
        Mockito.verifyNoMoreInteractions(visibleScreenTracker);
    }

    @Test
    void prePaused() {
        Pre29VisibleScreenLifecycleBinding underTest =
                new Pre29VisibleScreenLifecycleBinding(visibleScreenTracker);
        underTest.onActivityPaused(activity);
        verify(visibleScreenTracker).activityPaused(activity);
        Mockito.verifyNoMoreInteractions(visibleScreenTracker);
    }
}
