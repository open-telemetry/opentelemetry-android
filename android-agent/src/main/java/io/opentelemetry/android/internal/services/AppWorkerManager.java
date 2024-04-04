/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWorkAppWorker;
import java.util.HashMap;
import java.util.Map;

/**
 * Manging the app worker with singleton pattern.
 */
public final class AppWorkerManager implements AppWorking {

    private final Map<Class<? extends AppWorker>, AppWorker> services = new HashMap<>();
    @Nullable private static AppWorkerManager instance;

    @VisibleForTesting
    AppWorkerManager() {}

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static void initialize(Context appContext) {
        if (instance != null) {
            // Already initialized.
            return;
        }
        instance = new AppWorkerManager();
        instance.addService(PreferencesAppWorker.create(appContext));
        instance.addService(new CacheStorageAppWorker(appContext));
        instance.addService(new PeriodicWorkAppWorker());
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static AppWorkerManager get() {
        if (instance == null) {
            throw new IllegalStateException("Services haven't been initialized");
        }
        return instance;
    }

    public <T extends AppWorker> void addService(T service) {
        Class<? extends AppWorker> type = service.getClass();
        verifyNotExisting(type);
        services.put(type, service);
    }

    @Override
    public void start() {
        for (AppWorker appWorker : services.values()) {
            appWorker.start();
        }
    }

    @Override
    public void stop() {
        for (AppWorker appWorker : services.values()) {
            appWorker.stop();
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public <T extends AppWorker> T getService(Class<T> type) {
        AppWorker appWorker = services.get(type);
        if (appWorker == null) {
            throw new IllegalArgumentException("Service not found: " + type);
        }

        return (T) appWorker;
    }

    public static void resetForTest() {
        instance = null;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static void setForTest(AppWorkerManager appWorkerManager) {
        instance = appWorkerManager;
    }

    private void verifyNotExisting(Class<? extends AppWorker> type) {
        if (services.containsKey(type)) {
            throw new IllegalArgumentException("Service already registered with type: " + type);
        }
    }
}
