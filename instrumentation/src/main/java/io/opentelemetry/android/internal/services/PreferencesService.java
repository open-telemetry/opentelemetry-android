/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services;

import android.content.Context;
import android.content.SharedPreferences;
import io.opentelemetry.android.BuildConfig;

/**
 * Utility to store and retrieve apps' preferences.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
public class PreferencesService implements Service {
    private final SharedPreferences preferences;

    public PreferencesService(Context context) {
        preferences =
                context.getSharedPreferences(
                        BuildConfig.LIBRARY_PACKAGE_NAME + ".prefs", Context.MODE_PRIVATE);
    }

    @Override
    public void start() {
        // No op
    }

    @Override
    public void stop() {
        // No op
    }

    public void store(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }

    public int retrieveInt(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    @Override
    public Type type() {
        return Type.PREFERENCES;
    }
}
