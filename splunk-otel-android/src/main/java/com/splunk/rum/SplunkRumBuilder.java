/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum;

import static com.splunk.rum.DeviceSpanStorageLimiter.DEFAULT_MAX_STORAGE_USE_MB;

import android.app.Application;
import android.util.Log;
import androidx.annotation.Nullable;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.rum.internal.instrumentation.network.CurrentNetworkProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Duration;
import java.util.function.Consumer;

/** A builder of {@link SplunkRum}. */
public final class SplunkRumBuilder {

    private static final Duration DEFAULT_SLOW_RENDERING_DETECTION_POLL_INTERVAL =
            Duration.ofSeconds(1);

    @Nullable String applicationName;
    @Nullable String beaconEndpoint;
    @Nullable String rumAccessToken;
    @Nullable private String realm;
    boolean debugEnabled = false;
    boolean diskBufferingEnabled = false;
    boolean reactNativeSupportEnabled = false;
    boolean crashReportingEnabled = true;
    boolean networkMonitorEnabled = true;
    boolean anrDetectionEnabled = true;
    boolean slowRenderingDetectionEnabled = true;
    Duration slowRenderingDetectionPollInterval = DEFAULT_SLOW_RENDERING_DETECTION_POLL_INTERVAL;
    Attributes globalAttributes = Attributes.empty();
    @Nullable String deploymentEnvironment;
    private final SpanFilterBuilder spanFilterBuilder = new SpanFilterBuilder();
    int maxUsageMegabytes = DEFAULT_MAX_STORAGE_USE_MB;
    boolean sessionBasedSamplerEnabled = false;
    double sessionBasedSamplerRatio = 1.0;

    /**
     * Sets the application name that will be used to identify your application in the Splunk RUM
     * UI.
     *
     * @return {@code this}
     */
    public SplunkRumBuilder setApplicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    /**
     * Sets the "beacon" endpoint URL to be used by the RUM library.
     *
     * <p>Note that if you are using standard Splunk ingest, it is simpler to just use {@link
     * #setRealm(String)} and let this configuration set the full endpoint URL for you.
     *
     * @return {@code this}
     */
    public SplunkRumBuilder setBeaconEndpoint(String beaconEndpoint) {
        if (realm != null) {
            Log.w(
                    SplunkRum.LOG_TAG,
                    "Explicitly setting the beaconEndpoint will override the realm configuration.");
            realm = null;
        }
        this.beaconEndpoint = beaconEndpoint;
        return this;
    }

    /**
     * Sets the realm for the beacon to send RUM telemetry to. This should be used in place of the
     * {@link #setBeaconEndpoint(String)} method in most cases.
     *
     * @param realm A valid Splunk "realm", e.g. "us0", "eu0".
     * @return {@code this}
     */
    public SplunkRumBuilder setRealm(String realm) {
        if (beaconEndpoint != null && this.realm == null) {
            Log.w(
                    SplunkRum.LOG_TAG,
                    "beaconEndpoint has already been set. Realm configuration will be ignored.");
            return this;
        }
        this.beaconEndpoint = "https://rum-ingest." + realm + ".signalfx.com/v1/rum";
        this.realm = realm;
        return this;
    }

    /**
     * Sets the RUM auth token to be used by the RUM library.
     *
     * @return {@code this}
     */
    public SplunkRumBuilder setRumAccessToken(String rumAuthToken) {
        this.rumAccessToken = rumAuthToken;
        return this;
    }

    /**
     * Enables debugging information to be emitted from the RUM library.
     *
     * <p>This feature is disabled by default. You can enable it by calling this method.
     *
     * @return {@code this}
     */
    public SplunkRumBuilder enableDebug() {
        this.debugEnabled = true;
        return this;
    }

    /**
     * Enables the storage-based buffering of telemetry. If this feature is enabled, telemetry is
     * buffered in the local storage until it is exported; otherwise, it is buffered in memory and
     * throttled.
     *
     * <p>This feature is disabled by default. You can enable it by calling this method.
     *
     * @return {@code this}
     */
    public SplunkRumBuilder enableDiskBuffering() {
        this.diskBufferingEnabled = true;
        return this;
    }

    /**
     * Enables support for the React Native instrumentation.
     *
     * <p>This feature is disabled by default. You can enable it by calling this method.
     *
     * @return {@code this}
     */
    public SplunkRumBuilder enableReactNativeSupport() {
        this.reactNativeSupportEnabled = true;
        return this;
    }

    /**
     * Disables the crash reporting feature.
     *
     * <p>This feature is enabled by default. You can disable it by calling this method.
     *
     * @return {@code this}
     */
    public SplunkRumBuilder disableCrashReporting() {
        this.crashReportingEnabled = false;
        return this;
    }

    /**
     * Disables the network monitoring feature.
     *
     * <p>This feature is enabled by default. You can disable it by calling this method.
     *
     * @return {@code this}
     */
    public SplunkRumBuilder disableNetworkMonitor() {
        this.networkMonitorEnabled = false;
        return this;
    }

    /**
     * Disables the ANR (application not responding) detection feature. If enabled, when the main
     * thread is unresponsive for 5 seconds or more, an event including the main thread's stack
     * trace will be reported to the RUM system.
     *
     * <p>This feature is enabled by default. You can disable it by calling this method.
     *
     * @return {@code this}
     */
    public SplunkRumBuilder disableAnrDetection() {
        this.anrDetectionEnabled = false;
        return this;
    }

    /**
     * Disables the slow rendering detection feature.
     *
     * <p>This feature is enabled by default. You can disable it by calling this method.
     *
     * @return {@code this}
     */
    public SplunkRumBuilder disableSlowRenderingDetection() {
        slowRenderingDetectionEnabled = false;
        return this;
    }

    /**
     * Configures the rate at which frame render durations are polled.
     *
     * @param interval The period that should be used for polling.
     * @return {@code this}
     */
    public SplunkRumBuilder setSlowRenderingDetectionPollInterval(Duration interval) {
        if (interval.toMillis() <= 0) {
            Log.e(
                    SplunkRum.LOG_TAG,
                    "invalid slowRenderPollingDuration: " + interval + " is not positive");
            return this;
        }
        this.slowRenderingDetectionPollInterval = interval;
        return this;
    }

    /**
     * Provides a set of global {@link Attributes} that will be applied to every span generated by
     * the RUM instrumentation.
     *
     * @return {@code this}
     */
    public SplunkRumBuilder setGlobalAttributes(Attributes attributes) {
        this.globalAttributes = attributes == null ? Attributes.empty() : attributes;
        return this;
    }

    /**
     * Sets the deployment environment for this RUM instance. Deployment environment is passed along
     * as a span attribute to help identify in the Splunk RUM UI.
     *
     * @param environment The deployment environment name
     * @return {@code this}
     */
    public SplunkRumBuilder setDeploymentEnvironment(String environment) {
        this.deploymentEnvironment = environment;
        return this;
    }

    /**
     * Configures span data filtering.
     *
     * @param configurer A function that will configure the passed {@link SpanFilterBuilder}.
     * @return {@code this}
     */
    public SplunkRumBuilder filterSpans(Consumer<SpanFilterBuilder> configurer) {
        configurer.accept(spanFilterBuilder);
        return this;
    }

    /**
     * Sets the limit of the max number of megabytes that will be used to buffer telemetry data in
     * storage. When this value is exceeded, older telemetry will be deleted until the usage is
     * reduced.
     *
     * <p>This setting only applies when {@linkplain #enableDiskBuffering() disk buffering is
     * enabled}.
     *
     * @param maxUsageMegabytes The maximum disk buffer size, in megabytes.
     * @return {@code this}
     */
    public SplunkRumBuilder limitDiskUsageMegabytes(int maxUsageMegabytes) {
        this.maxUsageMegabytes = maxUsageMegabytes;
        return this;
    }

    /**
     * Sets the ratio of sessions that get sampled. Valid values range from 0.0 to 1.0, where 0
     * means no sessions are sampled, and 1 means all sessions are sampled.
     *
     * <p>This feature is disabled by default - i.e. by default, all sessions are sampled, which is
     * equivalent to {@code ratio = 1.0}.
     *
     * @param ratio The desired ratio of sampling. Must be within [0.0, 1.0].
     * @return {@code this}
     */
    public SplunkRumBuilder enableSessionBasedSampling(double ratio) {
        if (ratio < 0.0) {
            Log.e(
                    SplunkRum.LOG_TAG,
                    "invalid sessionBasedSamplingRatio: " + ratio + " must not be negative");
            return this;
        } else if (ratio > 1.0) {
            Log.e(
                    SplunkRum.LOG_TAG,
                    "invalid sessionBasedSamplingRatio: "
                            + ratio
                            + " must not be greater than 1.0");
            return this;
        }

        this.sessionBasedSamplerEnabled = true;
        this.sessionBasedSamplerRatio = ratio;
        return this;
    }

    /**
     * Creates a new instance of {@link SplunkRum} with the settings of this {@link
     * SplunkRumBuilder}.
     *
     * <p>You must configure at least the {@linkplain #setApplicationName(String) application name},
     * the {@linkplain #setRealm(String) realm} or the {@linkplain #setBeaconEndpoint(String) beacon
     * endpoint}, and the {@linkplain #setRumAccessToken(String) access token} before calling this
     * method. Trying to build a {@link SplunkRum} instance without any of these will result in an
     * exception being thrown.
     *
     * <p>The returned {@link SplunkRum} is set as the global instance {@link
     * SplunkRum#getInstance()}. If there was a global {@link SplunkRum} instance configured before,
     * this method does not initialize a new one and simply returns the existing instance.
     */
    public SplunkRum build(Application application) {
        if (rumAccessToken == null || beaconEndpoint == null || applicationName == null) {
            throw new IllegalStateException(
                    "You must provide a rumAccessToken, a realm (or full beaconEndpoint), and an applicationName to create a valid Config instance.");
        }
        return SplunkRum.initialize(this, application, CurrentNetworkProvider::createAndStart);
    }

    SpanExporter decorateWithSpanFilter(SpanExporter exporter) {
        return spanFilterBuilder.build().apply(exporter);
    }
}
