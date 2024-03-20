package io.opentelemetry.instrumentation.library.okhttp.v3_0;

import androidx.annotation.NonNull;

import java.io.IOException;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

class TestInjectingInterceptor implements Interceptor {
    @NonNull
    @Override
   public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder requestBuilder = originalRequest.newBuilder();
        // Retrieve current context and baggage
        Context currentContext = Context.current();
        Baggage currentBaggage = Baggage.fromContext(currentContext);
        // Inject baggage into request headers
        currentBaggage.forEach((key, value) -> requestBuilder.addHeader(key, value.getValue()));
        requestBuilder.addHeader("fixed_header_key","fixed_header_value");
        Request requestWithBaggage = requestBuilder.build();
        return chain.proceed(requestWithBaggage);
    }
}