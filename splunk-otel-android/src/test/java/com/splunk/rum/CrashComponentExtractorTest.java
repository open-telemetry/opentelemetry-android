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

import static com.splunk.rum.SplunkRum.COMPONENT_KEY;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import org.junit.jupiter.api.Test;

class CrashComponentExtractorTest {

    @Test
    void shouldSetComponentCrashOnTheFirstInvocation() {
        CrashComponentExtractor extractor = new CrashComponentExtractor();

        {
            AttributesBuilder builder = Attributes.builder();
            extractor.onStart(builder, null, null);
            assertThat(builder.build())
                    .hasSize(1)
                    .containsEntry(COMPONENT_KEY, SplunkRum.COMPONENT_CRASH);
        }

        {
            AttributesBuilder builder = Attributes.builder();
            extractor.onStart(builder, null, null);
            assertThat(builder.build())
                    .hasSize(1)
                    .containsEntry(COMPONENT_KEY, SplunkRum.COMPONENT_ERROR);
        }
    }
}
