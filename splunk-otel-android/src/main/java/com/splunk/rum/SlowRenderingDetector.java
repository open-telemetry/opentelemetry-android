package com.splunk.rum;

import android.app.Activity;

interface SlowRenderingDetector {
    SlowRenderingDetector NO_OP = new NoOp();

    void add(Activity activity);

    void stop(Activity activity);

    void start();

    class NoOp implements SlowRenderingDetector {

        @Override
        public void add(Activity activity) {
        }

        @Override
        public void stop(Activity activity) {
        }

        @Override
        public void start() {
        }
    }
}
