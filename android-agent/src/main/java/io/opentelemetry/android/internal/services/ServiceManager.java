/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services;

import android.app.Application;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycleService;
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWorkService;
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
public final class ServiceManager implements Lifecycle {

    private final Map<Class<? extends Service>, Service> services = new HashMap<>();
    @Nullable private static ServiceManager instance;

    @VisibleForTesting
    ServiceManager() {}

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static void initialize(Application application) {
        if (instance != null) {
            // Already initialized.
            return;
        }
        instance = new ServiceManager();
        instance.addService(PreferencesService.create(application));
        instance.addService(new CacheStorageService(application));
        instance.addService(new PeriodicWorkService());
        instance.addService(AppLifecycleService.create());
        instance.addService(VisibleScreenService.create(application));
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static ServiceManager get() {
        if (instance == null) {
            throw new IllegalStateException("Services haven't been initialized");
        }
        return instance;
    }

    public <T extends Service> void addService(T service) {
        Class<? extends Service> type = service.getClass();
        verifyNotExisting(type);
        services.put(type, service);
    }

    @Override
    public void start() {
        for (Service service : services.values()) {
            service.start();
        }
    }

    @Override
    public void stop() {
        for (Service service : services.values()) {
            service.stop();
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public <T extends Service> T getService(Class<T> type) {
        Service service = services.get(type);
        if (service == null) {
            throw new IllegalArgumentException("Service not found: " + type);
        }

        return (T) service;
    }

    public static void resetForTest() {
        instance = null;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static void setForTest(ServiceManager serviceManager) {
        instance = serviceManager;
    }

    private void verifyNotExisting(Class<? extends Service> type) {
        if (services.containsKey(type)) {
            throw new IllegalArgumentException("Service already registered with type: " + type);
        }
    }
}
