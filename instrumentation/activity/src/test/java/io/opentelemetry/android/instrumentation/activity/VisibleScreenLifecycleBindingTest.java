/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.app.Activity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VisibleScreenLifecycleBindingTest {

    @Mock Activity activity;
    @Mock VisibleScreenTracker tracker;

    @Test
    void postResumed() {
        VisibleScreenLifecycleBinding underTest = new VisibleScreenLifecycleBinding(tracker);
        underTest.onActivityPostResumed(activity);
        verify(tracker).activityResumed(activity);
        Mockito.verifyNoMoreInteractions(tracker);
    }

    @Test
    void prePaused() {
        VisibleScreenLifecycleBinding underTest = new VisibleScreenLifecycleBinding(tracker);
        underTest.onActivityPrePaused(activity);
        verify(tracker).activityPaused(activity);
    }
}
