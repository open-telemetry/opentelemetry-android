/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentelemetry.android.internal.services.periodicwork.PeriodicWorkAppWorker;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AppWorkerManagerTest {

    private AppWorkerManager appWorkerManager;

    @Before
    public void setUp() {
        appWorkerManager = new AppWorkerManager();
    }

    @Test
    public void addingServices() {
        FirstAppWorker firstService = mock();
        SecondAppWorker secondService = mock();

        appWorkerManager.addService(firstService);
        appWorkerManager.addService(secondService);

        assertEquals(firstService, appWorkerManager.getService(FirstAppWorker.class));
        assertEquals(secondService, appWorkerManager.getService(SecondAppWorker.class));
    }

    @Test
    public void allowOnlyOneServicePerType() {
        FirstAppWorker firstService = mock();
        FirstAppWorker anotherFirstService = mock();

        appWorkerManager.addService(firstService);

        try {
            appWorkerManager.addService(anotherFirstService);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void delegatingStartCall() {
        FirstAppWorker firstService = mock();
        SecondAppWorker secondService = mock();
        appWorkerManager.addService(firstService);
        appWorkerManager.addService(secondService);

        appWorkerManager.start();

        verify(firstService).start();
        verify(secondService).start();
    }

    @Test
    public void delegatingStopCall() {
        FirstAppWorker firstService = mock();
        SecondAppWorker secondService = mock();
        appWorkerManager.addService(firstService);
        appWorkerManager.addService(secondService);

        appWorkerManager.stop();

        verify(firstService).stop();
        verify(secondService).stop();
    }

    @Test
    public void validateRegisteredServices() {
        AppWorkerManager.resetForTest();
        List<Class<? extends AppWorker>> expectedServices = new ArrayList<>();
        expectedServices.add(PreferencesAppWorker.class);
        expectedServices.add(CacheStorageAppWorker.class);
        expectedServices.add(PeriodicWorkAppWorker.class);

        AppWorkerManager.initialize(RuntimeEnvironment.getApplication());

        for (Class<? extends AppWorker> expectedService : expectedServices) {
            assertThat(AppWorkerManager.get().getService(expectedService)).isNotNull();
        }
    }

    private static class FirstAppWorker implements AppWorker {}

    private static class SecondAppWorker implements AppWorker {}
}
