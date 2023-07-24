/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NetworkAttributesSpanAppenderTest {

    @Mock
    CurrentNetworkProvider currentNetworkProvider;
    @Mock ReadWriteSpan span;

    @InjectMocks
    NetworkAttributesSpanAppender underTest;

    @Test
    void shouldAppendNetworkAttributes() {
        when(currentNetworkProvider.getCurrentNetwork())
                .thenReturn(
                        CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                                .subType("LTE")
                                .build());

        assertTrue(underTest.isStartRequired());
        underTest.onStart(Context.current(), span);

        verify(span)
                .setAllAttributes(
                        Attributes.of(
                                SemanticAttributes.NET_HOST_CONNECTION_TYPE, "cell",
                                SemanticAttributes.NET_HOST_CONNECTION_SUBTYPE, "LTE"));

        assertFalse(underTest.isEndRequired());
    }
}
