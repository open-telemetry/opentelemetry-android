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

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.trace.Span;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This currently allows handling exceptions during the request and recording exception attributes to the
 * RUM http span. Hopefully the otel instrumentation will support this use-case soon and we won't
 * need to do these hijinks to capture this information.
 */
class OkHttpRumInterceptor implements Interceptor {

    private final Interceptor coreInterceptor;

    public OkHttpRumInterceptor(Interceptor coreInterceptor) {
        this.coreInterceptor = coreInterceptor;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        return coreInterceptor.intercept(new DelegatingChain(chain));
    }

    /**
     * A {@link okhttp3.Interceptor.Chain} implementation that allows delegating to another interceptor.
     */
    private static class DelegatingChain implements Chain {
        private final Chain chain;

        public DelegatingChain(Chain chain) {
            this.chain = chain;
        }

        @NotNull
        @Override
        public Request request() {
            return chain.request();
        }

        @NotNull
        @Override
        public Response proceed(@NotNull Request request) throws IOException {
            Response response;
            try {
                response = chain.proceed(request);
            } catch (IOException e) {
                SplunkRum.addExceptionAttributes(Span.current(), e);
                throw e;
            }
            return response;
        }

        @Override
        public Connection connection() {
            return chain.connection();
        }

        @NotNull
        @Override
        public Call call() {
            return chain.call();
        }

        @Override
        public int connectTimeoutMillis() {
            return chain.connectTimeoutMillis();
        }

        @NotNull
        @Override
        public Chain withConnectTimeout(int i, @NotNull TimeUnit timeUnit) {
            return chain.withConnectTimeout(i, timeUnit);
        }

        @Override
        public int readTimeoutMillis() {
            return chain.readTimeoutMillis();
        }

        @NotNull
        @Override
        public Chain withReadTimeout(int i, @NotNull TimeUnit timeUnit) {
            return chain.withReadTimeout(i, timeUnit);
        }

        @Override
        public int writeTimeoutMillis() {
            return chain.writeTimeoutMillis();
        }

        @NotNull
        @Override
        public Chain withWriteTimeout(int i, @NotNull TimeUnit timeUnit) {
            return chain.withWriteTimeout(i, timeUnit);
        }
    }

}
