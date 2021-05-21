package com.splunk.rum;

interface TrackableTracer {

    TrackableTracer NO_OP_TRACER = new NoOpTracer();

    TrackableTracer startSpanIfNoneInProgress(String action);

    TrackableTracer startTrackableCreation();

    TrackableTracer addEvent(String eventName);

    void endSpanForActivityResumed();

    void endActiveSpan();

    class NoOpTracer implements TrackableTracer {

        @Override
        public TrackableTracer startSpanIfNoneInProgress(String action) {
            return this;
        }

        @Override
        public TrackableTracer startTrackableCreation() {
            return this;
        }

        @Override
        public void endSpanForActivityResumed() {
        }

        @Override
        public void endActiveSpan() {
        }

        @Override
        public TrackableTracer addEvent(String eventName) {
            return this;
        }
    }
}
