/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.applifecycle;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

import androidx.core.app.ComponentActivity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @noinspection KotlinInternalInJava
 */
@ExtendWith(MockitoExtension.class)
class ApplicationStateWatcherTest {

    @Mock ComponentActivity activity;
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
        underTest.onStart(activity);

        InOrder io = inOrder(listener1, listener2);
        io.verify(listener1).onApplicationForegrounded();
        io.verify(listener2).onApplicationForegrounded();
        io.verifyNoMoreInteractions();
    }

    @Test
    void appBackgrounded() {
        underTest.onStart(activity);
        underTest.onStop(activity);

        InOrder io = inOrder(listener1, listener2);
        io.verify(listener1).onApplicationForegrounded();
        io.verify(listener2).onApplicationForegrounded();
        io.verify(listener1).onApplicationBackgrounded();
        io.verify(listener2).onApplicationBackgrounded();
        io.verifyNoMoreInteractions();
    }

    @Test
    void closing() {
        underTest.close();

        underTest.onStart(activity);
        underTest.onStop(activity);

        verifyNoInteractions(listener1, listener2);
    }
}
