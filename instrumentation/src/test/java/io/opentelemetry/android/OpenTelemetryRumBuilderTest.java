/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.android.RumConstants.SESSION_ID_KEY;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import io.opentelemetry.android.instrumentation.ApplicationStateListener;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenTelemetryRumBuilderTest {

    final Resource resource =
            Resource.getDefault().toBuilder().put("test.attribute", "abcdef").build();
    final InMemorySpanExporter spanExporter = InMemorySpanExporter.create();

    @Mock Application application;
    @Mock Activity activity;
    @Mock ApplicationStateListener listener;

    @Captor ArgumentCaptor<Application.ActivityLifecycleCallbacks> activityCallbacksCaptor;

    @Test
    void shouldRegisterApplicationStateWatcher() {
        OpenTelemetryRum.builder(application).build();

        verify(application).registerActivityLifecycleCallbacks(isA(ApplicationStateWatcher.class));
    }

    @Test
    void shouldBuildTracerProvider() {
        OpenTelemetryRum openTelemetryRum =
                OpenTelemetryRum.builder(application)
                        .setResource(resource)
                        .addTracerProviderCustomizer(
                                (tracerProviderBuilder, app) ->
                                        tracerProviderBuilder.addSpanProcessor(
                                                SimpleSpanProcessor.create(spanExporter)))
                        .build();

        String sessionId = openTelemetryRum.getRumSessionId();
        openTelemetryRum
                .getOpenTelemetry()
                .getTracer("test")
                .spanBuilder("test span")
                .startSpan()
                .end();

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0))
                .hasName("test span")
                .hasResource(resource)
                .hasAttributesSatisfyingExactly(equalTo(SESSION_ID_KEY, sessionId));
    }

    @Test
    void shouldInstallInstrumentation() {
        OpenTelemetryRum.builder(application)
                .addInstrumentation(
                        instrumentedApplication -> {
                            assertThat(instrumentedApplication.getApplication())
                                    .isSameAs(application);
                            instrumentedApplication.registerApplicationStateListener(listener);
                        })
                .build();

        verify(application).registerActivityLifecycleCallbacks(activityCallbacksCaptor.capture());

        activityCallbacksCaptor.getValue().onActivityStarted(activity);
        verify(listener).onApplicationForegrounded();

        activityCallbacksCaptor.getValue().onActivityStopped(activity);
        verify(listener).onApplicationBackgrounded();
    }

    @Test
    void canAddPropagator() {
        Context context = Context.root();
        Object carrier = new Object();

        Context expected = mock(Context.class);
        TextMapGetter<? super Object> getter = mock(TextMapGetter.class);
        TextMapPropagator customPropagator = mock(TextMapPropagator.class);

        when(customPropagator.extract(context, carrier, getter)).thenReturn(expected);

        OpenTelemetryRum rum =
                OpenTelemetryRum.builder(application)
                        .addPropagatorCustomizer(x -> customPropagator)
                        .build();
        Context result =
                rum.getOpenTelemetry()
                        .getPropagators()
                        .getTextMapPropagator()
                        .extract(context, carrier, getter);
        assertThat(result).isSameAs(expected);
    }

    @Test
    void canSetPropagator() {
        TextMapPropagator customPropagator = mock(TextMapPropagator.class);

        OpenTelemetryRum rum =
                OpenTelemetryRum.builder(application)
                        .addPropagatorCustomizer(x -> customPropagator)
                        .build();
        TextMapPropagator result = rum.getOpenTelemetry().getPropagators().getTextMapPropagator();
        assertThat(result).isSameAs(customPropagator);
    }
}
