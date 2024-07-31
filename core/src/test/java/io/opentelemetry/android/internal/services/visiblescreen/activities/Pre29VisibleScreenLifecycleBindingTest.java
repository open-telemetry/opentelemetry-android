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
class Pre29VisibleScreenLifecycleBindingTest {
    @Mock Activity activity;
    @Mock VisibleScreenService visibleScreenService;

    @Test
    void postResumed() {
        Pre29VisibleScreenLifecycleBinding underTest =
                new Pre29VisibleScreenLifecycleBinding(visibleScreenService);
        underTest.onActivityResumed(activity);
        verify(visibleScreenService).activityResumed(activity);
        Mockito.verifyNoMoreInteractions(visibleScreenService);
    }

    @Test
    void prePaused() {
        Pre29VisibleScreenLifecycleBinding underTest =
                new Pre29VisibleScreenLifecycleBinding(visibleScreenService);
        underTest.onActivityPaused(activity);
        verify(visibleScreenService).activityPaused(activity);
        Mockito.verifyNoMoreInteractions(visibleScreenService);
    }
}
