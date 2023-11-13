package io.opentelemetry.android.internal.services;

import android.content.Context;

import androidx.annotation.RestrictTo;

import java.util.HashMap;

public final class ServiceManager implements Lifecycle {

    private final HashMap<Service.Type, Service> services = new HashMap<>();
    private static ServiceManager INSTANCE;

    private ServiceManager() {
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static void initialize(Context appContext) {
        INSTANCE = new ServiceManager();
        INSTANCE.addService(new PreferencesService(appContext));
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static ServiceManager get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Services haven't been initialized");
        }
        return INSTANCE;
    }

    public void addService(Service service) {
        Service.Type type = service.type();
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
    @SuppressWarnings("unchecked")
    public <T extends Service> T getService(Service.Type type) {
        Service service = services.get(type);
        if (service == null) {
            throw new IllegalArgumentException("Service not found: " + type);
        }

        return (T) service;
    }

    public static void resetForTest() {
        if (INSTANCE != null) {
            INSTANCE.stop();
        }
        INSTANCE = null;
    }

    private void verifyNotExisting(Service.Type type) {
        if (services.containsKey(type)) {
            throw new IllegalArgumentException("Service already registered with type: " + type);
        }
    }
}
