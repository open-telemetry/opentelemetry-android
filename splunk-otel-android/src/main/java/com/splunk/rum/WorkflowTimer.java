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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

/**
 * A WorkflowTimer is a convenience API for creating timed operations using the OpenTelemetry APIs.
 * It wraps an OpenTelemetry span, and provides a simpler, smaller API specifically oriented toward
 * tracking RUM "workflows".
 */
public class WorkflowTimer implements AutoCloseable {
    private static final AttributeKey<String> WORKFLOW_NAME_KEY = stringKey("workflow.name");

    private final Span span;
    private final Scope scope;

    WorkflowTimer(Span span, Scope scope) {
        this.span = span;
        this.scope = scope;
    }

    static WorkflowTimer create(Tracer tracer, String workflowName) {
        Span span = tracer.spanBuilder(workflowName)
                .setAttribute(WORKFLOW_NAME_KEY, workflowName)
                .startSpan();
        Scope scope = span.makeCurrent();
        return new WorkflowTimer(span, scope);
    }

    /**
     * Adds a named "point in time" event to the in-progress workflow. Note, this will be a no-op
     * after {@link #end()} or {@link #close()} have been called.
     *
     * @param name The name of the event.
     * @return this.
     */
    public WorkflowTimer addEvent(String name) {
        span.addEvent(name);
        return this;
    }

    /**
     * Adds a named "point in time" event to the in-progress workflow. Note, this will be a no-op
     * after {@link #end()} or {@link #close()} have been called.
     *
     * @param name       The name of the event.
     * @param attributes A set of attributes to add to the event.
     * @return this.
     */
    public WorkflowTimer addEvent(String name, Attributes attributes) {
        span.addEvent(name, attributes);
        return this;
    }

    /**
     * Sets a string-valued attribute on the Span that results from this workflow.
     *
     * @return this.
     * @see Span#setAttribute(String, String)
     */
    public WorkflowTimer setAttribute(String key, String value) {
        span.setAttribute(key, value);
        return this;
    }

    /**
     * Sets a long-valued attribute on the Span that results from this workflow.
     *
     * @return this.
     * @see Span#setAttribute(String, long)
     */
    public WorkflowTimer setAttribute(String key, long value) {
        span.setAttribute(key, value);
        return this;
    }

    /**
     * Sets a double-valued attribute on the Span that results from this workflow.
     *
     * @return this.
     * @see Span#setAttribute(String, double)
     */
    public WorkflowTimer setAttribute(String key, double value) {
        span.setAttribute(key, value);
        return this;
    }

    /**
     * Set2 a boolean-valued attribute on the Span that results from this workflow.
     *
     * @return this.
     * @see Span#setAttribute(String, boolean)
     */
    public WorkflowTimer setAttribute(String key, boolean value) {
        span.setAttribute(key, value);
        return this;
    }

    /**
     * Ends the workflow timer, and closes the OpenTelemetry thread-local {@link Scope} that
     * was associated with its execution.
     */
    @Override
    public void close() {
        end();
    }

    /**
     * Ends the workflow timer, and closes the OpenTelemetry thread-local {@link Scope} that
     * was associated with its execution.
     */
    public void end() {
        scope.close();
        span.end();
    }
}
