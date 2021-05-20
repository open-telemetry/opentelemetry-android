package com.splunk.rum;

interface TrackableTracer {

    TrackableTracer startSpanIfNoneInProgress(String action);

    TrackableTracer startActivityCreation();

    TrackableTracer addEvent(String eventName);

    void endSpanForActivityResumed();

    void endActiveSpan();
}
