/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import java.util.HashMap;
import java.util.Map;

public final class ServiceManager implements Lifecycle {

    private final Map<Class<? extends Service>, Service> services = new HashMap<>();
    @Nullable private static ServiceManager instance;

    private ServiceManager() {}

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static void initialize(Context appContext) {
        instance = new ServiceManager();
        instance.addService(PreferencesService.create(appContext));
        instance.addService(new CacheStorageService(appContext));
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
        if (instance != null) {
            instance.stop();
        }
        instance = null;
    }

    private void verifyNotExisting(Class<? extends Service> type) {
        if (services.containsKey(type)) {
            throw new IllegalArgumentException("Service already registered with type: " + type);
        }
    }
}
