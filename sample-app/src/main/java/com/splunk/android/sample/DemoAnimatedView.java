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

import android.animation.TimeAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class DemoAnimatedView extends androidx.appcompat.widget.AppCompatImageView {

    private final AtomicBoolean firstDraw = new AtomicBoolean(false);
    private final Paint paint = new Paint();
    private Bitmap bitmap;
    private Canvas canvas;
    private TimeAnimator timeAnimator;
    private long lastAnimationTime = 0;
    private final List<Signal> signals = buildSignals();
    private final AtomicBoolean slowly = new AtomicBoolean(false);

    public DemoAnimatedView(Context context) {
        super(context);
    }

    public DemoAnimatedView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (firstDraw.compareAndSet(false, true)) {
            init();
        }
        if (slowly.get()) {
            SecureRandom rand = new SecureRandom();
            for (int i = 0; i < 50000; i++) {
                rand.nextFloat();
            }
        }
        canvas.drawBitmap(bitmap, 0, 0, paint);
    }

    public void init() {
        if (getWidth() == 0) {
            return;
        }
        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint.setColor(Color.BLACK);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        timeAnimator = new TimeAnimator();
        timeAnimator.setTimeListener(
                (animation, totalTime, deltaTime) -> {
                    if (totalTime - lastAnimationTime > 25) {
                        drawBitmap(totalTime);
                        lastAnimationTime = totalTime;
                    }
                });
        timeAnimator.start();
    }

    private void drawBitmap(long time) {
        for (int i = 0; i < 5; i++) {
            Canvas tmpCanvas = new Canvas(bitmap);
            Rect src = new Rect(1, 0, getWidth(), getHeight());
            Rect dst = new Rect(0, 0, getWidth() - 1, getHeight());
            tmpCanvas.drawBitmap(bitmap, src, dst, paint);
            paint.setColor(Color.BLACK);
            tmpCanvas.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight(), paint);

            signals.stream()
                    .forEach(
                            signal -> {
                                paint.setColor(signal.color);
                                int ypos =
                                        (int)
                                                (Math.sin(0.001 * signal.rate * time + signal.phase)
                                                                * getHeight()
                                                                / 2
                                                        + (getHeight() / 2));
                                tmpCanvas.drawCircle(getWidth() - 1, ypos, 5, paint);
                            });
            canvas.drawBitmap(bitmap, 0, 0, paint);
        }
        setImageBitmap(bitmap);
    }

    private static List<Signal> buildSignals() {
        Random rand = new Random();
        return Arrays.asList(
                new Signal(randomColor(), 1.5f + rand.nextFloat() * 3, rand.nextFloat() - 0.5f),
                new Signal(randomColor(), 1.5f - rand.nextFloat() * 3, rand.nextFloat() - 0.5f),
                new Signal(randomColor(), 1.5f - rand.nextFloat() * 3, rand.nextFloat() - 0.5f),
                new Signal(randomColor(), 1.5f - rand.nextFloat() * 3, rand.nextFloat() - 0.5f),
                new Signal(randomColor(), 1.5f - rand.nextFloat() * 3, rand.nextFloat() - 0.5f));
    }

    private static int randomColor() {
        Random rand = new Random();
        return Color.rgb(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255));
    }

    public boolean toggleSlowly() {
        synchronized (slowly) {
            boolean newValue = !slowly.get();
            if (newValue) {
                Log.i("demo", "Starting to render more slowly...");
            } else {
                Log.i("demo", "Back to more normal rendering speed");
            }

            slowly.set(newValue);
            return newValue;
        }
    }

    static class Signal {

        final int color;
        final float rate;
        final float phase;

        Signal(int color, float rate, float phase) {
            this.color = color;
            this.rate = rate;
            this.phase = phase;
        }
    }
}
