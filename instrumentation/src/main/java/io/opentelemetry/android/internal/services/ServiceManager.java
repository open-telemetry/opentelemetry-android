package io.opentelemetry.android.internal.services;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.HashMap;

public final class ServiceManager implements Lifecycle {

    private final HashMap<Service.Type, Service> services = new HashMap<>();
    @Nullable
    private static ServiceManager instance;

    private ServiceManager() {
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static void initialize(Context appContext) {
        instance = new ServiceManager();
        instance.addService(new PreferencesService(appContext));
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static ServiceManager get() {
        if (instance == null) {
            throw new IllegalStateException("Services haven't been initialized");
        }
        return instance;
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
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public <T extends Service> T getService(Service.Type type) {
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

    private void verifyNotExisting(Service.Type type) {
        if (services.containsKey(type)) {
            throw new IllegalArgumentException("Service already registered with type: " + type);
        }
    }
}
