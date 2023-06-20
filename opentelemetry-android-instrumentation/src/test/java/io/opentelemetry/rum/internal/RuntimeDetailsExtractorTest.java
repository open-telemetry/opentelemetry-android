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

package io.opentelemetry.rum.internal;

import static io.opentelemetry.context.Context.root;
import static io.opentelemetry.rum.internal.RumConstants.BATTERY_PERCENT_KEY;
import static io.opentelemetry.rum.internal.RumConstants.HEAP_FREE_KEY;
import static io.opentelemetry.rum.internal.RumConstants.STORAGE_SPACE_FREE_KEY;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuntimeDetailsExtractorTest {

    @Mock Context context;
    @Mock Intent intent;
    @Mock File filesDir;

    @Test
    void shouldCollectRuntimeDetails() {
        when(context.getFilesDir()).thenReturn(filesDir);
        when(filesDir.getFreeSpace()).thenReturn(4200L);

        Integer level = 690;
        when(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(level);
        when(intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(1000);

        RuntimeDetailsExtractor<?, ?> details = RuntimeDetailsExtractor.create(context);
        details.onReceive(context, intent);

        AttributesBuilder attributes = Attributes.builder();
        details.onStart(attributes, root(), null);
        assertThat(attributes.build())
                .hasSize(3)
                .containsEntry(STORAGE_SPACE_FREE_KEY, 4200L)
                .containsKey(HEAP_FREE_KEY)
                .containsEntry(BATTERY_PERCENT_KEY, 69.0);
    }
}
