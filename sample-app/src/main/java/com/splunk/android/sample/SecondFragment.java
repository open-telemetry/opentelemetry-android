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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.fragment.NavHostFragment;

import com.splunk.android.sample.databinding.FragmentSecondBinding;
import com.splunk.rum.SplunkRum;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

public class SecondFragment extends Fragment {

    private final ScheduledExecutorService spammer = Executors.newSingleThreadScheduledExecutor();
    private final MutableLiveData<String> spanCountLabel = new MutableLiveData<>();
    private final AtomicLong spans = new AtomicLong(0);

    private ScheduledFuture<?> spamTask;

    private FragmentSecondBinding binding;
    private Tracer sampleAppTracer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        sampleAppTracer = SplunkRum.getInstance().getOpenTelemetry().getTracer("sampleAppTracer");
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setSecondFragment(this);

        resetLabel();

        binding.buttonSecond.setOnClickListener(v -> {
            //an example of using the OpenTelemetry API directly to generate a 100% custom span.
            Span span = sampleAppTracer
                    .spanBuilder("buttonClicked")
                    .setAttribute("buttonName", "backButton")
                    .startSpan();
            try (Scope s = span.makeCurrent()) {
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            } finally {
                span.end();
            }
        });
        binding.buttonToWebview.setOnClickListener(v ->
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_webViewFragment));
        binding.buttonSpam.setOnClickListener(v -> toggleSpam());

        binding.buttonFreeze.setOnClickListener(v -> {
            Span appFreezer = SplunkRum.getInstance().startWorkflow("app freezer");
            try {
                for (int i = 0; i < 20; i++) {
                    Thread.sleep(1_000);
                    appFreezer.addEvent("still sleeping");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                appFreezer.end();
            }
        });
        binding.buttonWork.setOnClickListener(v -> {
            Span hardWorker =
                    SplunkRum.getInstance().startWorkflow("main thread working hard");
            Random random = new Random();
            long startTime = System.currentTimeMillis();
            while (true) {
                random.nextDouble();
                if (System.currentTimeMillis() - startTime > 20_000) {
                    break;
                }
            }
            hardWorker.end();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public LiveData<String> getSpanCountLabel() {
        return spanCountLabel;
    }

    private void toggleSpam() {
        if (spamTask == null) {
            resetLabel();
            spamTask = spammer.scheduleAtFixedRate(this::createSpamSpan, 0, 50, TimeUnit.MILLISECONDS);
            binding.buttonSpam.setText(R.string.stop_spam);
        } else {
            spamTask.cancel(false);
            spamTask = null;
            binding.buttonSpam.setText(R.string.start_spam);
        }
    }

    private void resetLabel() {
        spans.set(0);
        updateLabel();
    }

    private void updateLabel() {
        spanCountLabel.postValue(getString(R.string.spam_status, spans.get()));
    }

    private void createSpamSpan() {
        sampleAppTracer.spanBuilder("spam span no. " + spans.incrementAndGet())
                .setAttribute("number", spans.get())
                .startSpan()
                .end();
        updateLabel();
    }
}