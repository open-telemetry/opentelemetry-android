/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.activities;

import static org.mockito.Mockito.verify;

import android.app.Activity;
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VisibleScreenLifecycleBindingTest {

    @Mock Activity activity;
    @Mock VisibleScreenService visibleScreenService;

    @Test
    void postResumed() {
        VisibleScreenLifecycleBinding underTest =
                new VisibleScreenLifecycleBinding(visibleScreenService);
        underTest.onActivityPostResumed(activity);
        verify(visibleScreenService).activityResumed(activity);
        Mockito.verifyNoMoreInteractions(visibleScreenService);
    }

    @Test
    void prePaused() {
        VisibleScreenLifecycleBinding underTest =
                new VisibleScreenLifecycleBinding(visibleScreenService);
        underTest.onActivityPrePaused(activity);
        verify(visibleScreenService).activityPaused(activity);
    }
}
