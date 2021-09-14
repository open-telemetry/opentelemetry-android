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

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

final class ModifiedSpanData implements SpanData {
    private final SpanData original;
    private final Attributes modifiedAttributes;

    ModifiedSpanData(SpanData original, Attributes modifiedAttributes) {
        this.original = original;
        this.modifiedAttributes = modifiedAttributes;
    }

    @Override
    public String getName() {
        return original.getName();
    }

    @Override
    public SpanKind getKind() {
        return original.getKind();
    }

    @Override
    public SpanContext getSpanContext() {
        return original.getSpanContext();
    }

    @Override
    public SpanContext getParentSpanContext() {
        return original.getParentSpanContext();
    }

    @Override
    public StatusData getStatus() {
        return original.getStatus();
    }

    @Override
    public long getStartEpochNanos() {
        return original.getStartEpochNanos();
    }

    @Override
    public Attributes getAttributes() {
        return modifiedAttributes;
    }

    @Override
    public List<EventData> getEvents() {
        return original.getEvents();
    }

    @Override
    public List<LinkData> getLinks() {
        return original.getLinks();
    }

    @Override
    public long getEndEpochNanos() {
        return original.getEndEpochNanos();
    }

    @Override
    public boolean hasEnded() {
        return original.hasEnded();
    }

    @Override
    public int getTotalRecordedEvents() {
        return original.getTotalRecordedEvents();
    }

    @Override
    public int getTotalRecordedLinks() {
        return original.getTotalRecordedLinks();
    }

    @Override
    public int getTotalAttributeCount() {
        // TODO: not sure about this
        return modifiedAttributes.size();
    }

    @Override
    public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
        return original.getInstrumentationLibraryInfo();
    }

    @Override
    public Resource getResource() {
        return original.getResource();
    }

    @Override
    public String toString() {
        return "ModifiedSpanData{" +
                "original=" + original +
                ", modifiedAttributes=" + modifiedAttributes +
                '}';
    }

    // TODO: hashCode, equals ?
}
