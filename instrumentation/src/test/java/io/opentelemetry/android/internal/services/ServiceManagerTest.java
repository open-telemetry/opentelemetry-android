/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceManagerTest {

    private ServiceManager serviceManager;

    @BeforeEach
    void setUp() {
        serviceManager = new ServiceManager();
    }

    @Test
    void addingServices() {
        FirstService firstService = mock();
        SecondService secondService = mock();

        serviceManager.addService(firstService);
        serviceManager.addService(secondService);

        Assertions.assertEquals(firstService, serviceManager.getService(FirstService.class));
        Assertions.assertEquals(secondService, serviceManager.getService(SecondService.class));
    }

    @Test
    void allowOnlyOneServicePerType() {
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
    void delegatingStartCall() {
        FirstService firstService = mock();
        SecondService secondService = mock();
        serviceManager.addService(firstService);
        serviceManager.addService(secondService);

        serviceManager.start();

        verify(firstService).start();
        verify(secondService).start();
    }

    @Test
    void delegatingStopCall() {
        FirstService firstService = mock();
        SecondService secondService = mock();
        serviceManager.addService(firstService);
        serviceManager.addService(secondService);

        serviceManager.stop();

        verify(firstService).stop();
        verify(secondService).stop();
    }

    private static class FirstService implements Service {}

    private static class SecondService implements Service {}
}
