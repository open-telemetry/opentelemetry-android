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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import com.splunk.android.sample.databinding.FragmentWebViewBinding;
import com.splunk.rum.SplunkRum;

import io.opentelemetry.api.common.Attributes;

/**
 * A simple {@link Fragment} subclass with a WebView in it.
 */
public class WebViewFragment extends Fragment {

    private FragmentWebViewBinding binding;
    private WebViewAssetLoader assetLoader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this.getContext()))
                .addPathHandler("/res/", new WebViewAssetLoader.ResourcesPathHandler(this.getContext()))
                .build();
        // Inflate the layout for this fragment
        this.binding = FragmentWebViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.webView.setWebViewClient(new LocalContentWebViewClient(assetLoader));
        binding.webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");

        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.addJavascriptInterface(new WebAppInterface(getContext()), "Android");
        SplunkRum.getInstance().integrateWithBrowserRum(binding.webView);
    }

    private static class LocalContentWebViewClient extends WebViewClientCompat {
        private final WebViewAssetLoader assetLoader;

        private LocalContentWebViewClient(WebViewAssetLoader assetLoader) {
            this.assetLoader = assetLoader;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return assetLoader.shouldInterceptRequest(request.getUrl());
        }
    }

    public static class WebAppInterface {
        private final Context context;

        public WebAppInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void showToast(String toast) {
            SplunkRum.getInstance().addRumEvent("WebViewButtonClicked", Attributes.empty());
            Toast.makeText(context, toast, Toast.LENGTH_LONG).show();
        }
    }
}