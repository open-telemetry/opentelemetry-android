/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.volley;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpResponse;
import com.android.volley.toolbox.HurlStack;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.io.IOException;
import java.util.Map;
import javax.net.ssl.SSLSocketFactory;

final class TracingHurlStack extends HurlStack {

    private final Instrumenter<RequestWrapper, HttpResponse> instrumenter;

    TracingHurlStack(Instrumenter<RequestWrapper, HttpResponse> instrumenter) {
        super();
        this.instrumenter = instrumenter;
    }

    TracingHurlStack(
            Instrumenter<RequestWrapper, HttpResponse> instrumenter,
            HurlStack.UrlRewriter urlRewriter) {
        super(urlRewriter);
        this.instrumenter = instrumenter;
    }

    TracingHurlStack(
            Instrumenter<RequestWrapper, HttpResponse> instrumenter,
            HurlStack.UrlRewriter urlRewriter,
            SSLSocketFactory sslSocketFactory) {
        super(urlRewriter, sslSocketFactory);
        this.instrumenter = instrumenter;
    }

    @Override
    public HttpResponse executeRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError {

        Context parentContext = Context.current();
        RequestWrapper requestWrapper = new RequestWrapper(request, additionalHeaders);

        if (!instrumenter.shouldStart(parentContext, requestWrapper)) {
            return super.executeRequest(request, additionalHeaders);
        }

        Context context = instrumenter.start(parentContext, requestWrapper);
        HttpResponse response = null;
        Throwable throwable = null;
        try (Scope ignored = context.makeCurrent()) {
            response = super.executeRequest(request, requestWrapper.getAdditionalHeaders());
            return response;
        } catch (Throwable t) {
            throwable = t;
            throw t;
        } finally {
            instrumenter.end(context, requestWrapper, response, throwable);
        }
    }
}
