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
class VisibleScreenLifecycleBindingTest {

    @Mock Activity activity;
    @Mock VisibleScreenTracker visibleScreenTracker;

    @Test
    void postResumed() {
        VisibleScreenLifecycleBinding underTest =
                new VisibleScreenLifecycleBinding(visibleScreenTracker);
        underTest.onActivityPostResumed(activity);
        verify(visibleScreenTracker).activityResumed(activity);
        Mockito.verifyNoMoreInteractions(visibleScreenTracker);
    }

    @Test
    void prePaused() {
        VisibleScreenLifecycleBinding underTest =
                new VisibleScreenLifecycleBinding(visibleScreenTracker);
        underTest.onActivityPrePaused(activity);
        verify(visibleScreenTracker).activityPaused(activity);
    }
}
