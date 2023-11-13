package io.opentelemetry.android.internal.services;

public interface Service extends Lifecycle {

    Type type();

    enum Type {
        APPLICATION_INFO, PREFERENCES
    }
}
