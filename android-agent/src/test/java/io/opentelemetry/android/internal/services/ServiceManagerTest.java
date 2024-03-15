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

import io.opentelemetry.android.internal.services.periodicwork.PeriodicWorkService;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ServiceManagerTest {

    private ServiceManager serviceManager;

    @Before
    public void setUp() {
        serviceManager = new ServiceManager();
    }

    @Test
    public void addingServices() {
        FirstService firstService = mock();
        SecondService secondService = mock();

        serviceManager.addService(firstService);
        serviceManager.addService(secondService);

        assertEquals(firstService, serviceManager.getService(FirstService.class));
        assertEquals(secondService, serviceManager.getService(SecondService.class));
    }

    @Test
    public void allowOnlyOneServicePerType() {
        FirstService firstService = mock();
        FirstService anotherFirstService = mock();

        serviceManager.addService(firstService);

        try {
            serviceManager.addService(anotherFirstService);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void delegatingStartCall() {
        FirstService firstService = mock();
        SecondService secondService = mock();
        serviceManager.addService(firstService);
        serviceManager.addService(secondService);

        serviceManager.start();

        verify(firstService).start();
        verify(secondService).start();
    }

    @Test
    public void delegatingStopCall() {
        FirstService firstService = mock();
        SecondService secondService = mock();
        serviceManager.addService(firstService);
        serviceManager.addService(secondService);

        serviceManager.stop();

        verify(firstService).stop();
        verify(secondService).stop();
    }

    @Test
    public void validateRegisteredServices() {
        ServiceManager.setForTest(null);
        List<Class<? extends Service>> expectedServices = new ArrayList<>();
        expectedServices.add(PreferencesService.class);
        expectedServices.add(CacheStorageService.class);
        expectedServices.add(PeriodicWorkService.class);

        ServiceManager.initialize(RuntimeEnvironment.getApplication());

        for (Class<? extends Service> expectedService : expectedServices) {
            assertThat(ServiceManager.get().getService(expectedService)).isNotNull();
        }
    }

    private static class FirstService implements Service {}

    private static class SecondService implements Service {}
}
