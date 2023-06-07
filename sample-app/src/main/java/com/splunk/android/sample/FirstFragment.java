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

package com.splunk.android.sample;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static org.apache.http.conn.ssl.SSLSocketFactory.SSL;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.fragment.NavHostFragment;
import com.splunk.android.sample.databinding.FragmentFirstBinding;
import com.splunk.rum.SplunkRum;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

public class FirstFragment extends Fragment {

    private final MutableLiveData<String> httpResponse = new MutableLiveData<>();
    private final MutableLiveData<String> sessionId = new MutableLiveData<>();

    private FragmentFirstBinding binding;
    private Call.Factory okHttpClient;
    private SplunkRum splunkRum;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        splunkRum = SplunkRum.getInstance();
        okHttpClient = buildOkHttpClient(splunkRum);
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setFirstFragment(this);

        binding.buttonFirst.setOnClickListener(
                v ->
                        NavHostFragment.findNavController(FirstFragment.this)
                                .navigate(R.id.action_FirstFragment_to_SecondFragment));

        binding.crash.setOnClickListener(v -> multiThreadCrashing());

        binding.httpMe.setOnClickListener(
                v -> {
                    Span workflow = splunkRum.startWorkflow("User Login");
                    // not really a login, but it does make an http call
                    makeCall("https://pmrum.o11ystore.com?user=me&pass=secret123secret", workflow);
                    // maybe this call gave us a real customer id, so let's put it into the global
                    // attributes
                    splunkRum.setGlobalAttribute(longKey("customerId"), 123456L);
                });
        binding.httpMeBad.setOnClickListener(
                v -> {
                    Span workflow = splunkRum.startWorkflow("Workflow With Error");
                    makeCall("https://asdlfkjasd.asdfkjasdf.ifi", workflow);
                });
        binding.httpMeNotFound.setOnClickListener(
                v -> {
                    Span workflow = splunkRum.startWorkflow("Workflow with 404");
                    makeCall("https://pmrum.o11ystore.com/foobarbaz", workflow);
                });

        binding.volleyRequest.setOnClickListener(
                v -> {
                    VolleyExample volleyExample = new VolleyExample(splunkRum);
                    volleyExample.doHttpRequest();
                });

        sessionId.postValue(splunkRum.getRumSessionId());
    }

    private void multiThreadCrashing() {
        CountDownLatch latch = new CountDownLatch(1);
        int numThreads = 4;
        for (int i = 0; i < numThreads; ++i) {
            Thread t =
                    new Thread(
                            () -> {
                                try {
                                    if (latch.await(10, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException(
                                                "Failure from thread "
                                                        + Thread.currentThread().getName());
                                    }
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            });
            t.setName("crash-thread-" + i);
            t.start();
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        latch.countDown();
    }

    private void makeCall(String url, Span workflow) {
        // make sure the span is in the current context so it can be propagated into the async call.
        try (Scope scope = workflow.makeCurrent()) {
            Call call = okHttpClient.newCall(new Request.Builder().url(url).get().build());
            call.enqueue(
                    new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            httpResponse.postValue("error");
                            workflow.setStatus(StatusCode.ERROR, "failure to communicate");
                            workflow.end();
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) {
                            try (ResponseBody body = response.body()) {
                                int responseCode = response.code();
                                httpResponse.postValue("" + responseCode);
                                workflow.end();
                            }
                        }
                    });
        }
    }

    public LiveData<String> getHttpResponse() {
        return httpResponse;
    }

    public LiveData<String> getSessionId() {
        return sessionId;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        sessionId.postValue(splunkRum.getRumSessionId());
    }

    @SuppressLint("AllowAllHostnameVerifier")
    private Call.Factory buildOkHttpClient(SplunkRum splunkRum) {
        // grab the default executor service that okhttp uses, and wrap it with one that will
        // propagate the otel context.
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        try {
            // NOTE: This is really bad and dangerous. Don't ever do this in the real world.
            // it's only necessary because the demo endpoint uses a self-signed SSL cert.
            SSLContext sslContext = SSLContext.getInstance(SSL);
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            return splunkRum.createRumOkHttpCallFactory(
                    builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                            .hostnameVerifier(new AllowAllHostnameVerifier())
                            .build());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
            return splunkRum.createRumOkHttpCallFactory(builder.build());
        }
    }

    @SuppressLint("CustomX509TrustManager")
    private static final TrustManager[] trustAllCerts =
            new TrustManager[] {
                new X509TrustManager() {

                    @SuppressLint("TrustAllX509TrustManager")
                    @Override
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] chain, String authType) {}

                    @SuppressLint("TrustAllX509TrustManager")
                    @Override
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] chain, String authType) {}

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[] {};
                    }
                }
            };

    // Much like the comment above, you should NEVER do something like this in production, it
    // is an extremely bad practice. This is only present because the demo endpoint uses a
    // self-signed SSL cert.
    static {
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((arg0, arg1) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
