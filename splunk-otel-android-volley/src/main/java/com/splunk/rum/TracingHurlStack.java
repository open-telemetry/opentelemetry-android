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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpResponse;
import com.android.volley.toolbox.HurlStack;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import javax.net.ssl.SSLSocketFactory;

final class TracingHurlStack extends HurlStack {

    private static final ThreadLocal<RequestWrapper> currentRequestWrapper = new ThreadLocal<>();
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

        HttpResponse response = null;
        Throwable throwable = null;

        RequestWrapper requestWrapper = new RequestWrapper(request, additionalHeaders);
        currentRequestWrapper.set(requestWrapper);

        Context parentContext = Context.current();
        Context context = null;
        Scope scope = null;

        if (instrumenter.shouldStart(parentContext, requestWrapper)) {
            context = instrumenter.start(parentContext, requestWrapper);
            scope = context.makeCurrent();
        }

        try {
            response = super.executeRequest(request, requestWrapper.getAdditionalHeaders());
        } catch (Throwable t) {
            throwable = t;
            throw t;
        } finally {
            if (scope != null) {
                scope.close();
                instrumenter.end(context, requestWrapper, response, throwable);
            }
            currentRequestWrapper.remove();
        }

        return response;
    }

    @Override
    protected HttpURLConnection createConnection(URL url) throws IOException {
        // requestWrapper cannot be null here, because this method is called only
        // inside parent's executeRequest() (through a private method - openConnection()),
        // so currentRequestWrapper.set() is always called before that
        RequestWrapper requestWrapper = currentRequestWrapper.get();
        requestWrapper.setUrl(url);
        return super.createConnection(url);
    }
}
