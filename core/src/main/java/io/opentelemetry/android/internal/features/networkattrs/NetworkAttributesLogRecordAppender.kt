/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.features.networkattrs

import io.opentelemetry.android.common.internal.features.networkattributes.CurrentNetworkAttributesExtractor
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

class NetworkAttributesLogRecordAppender(
    private val currentNetworkProvider: CurrentNetworkProvider,
    private val networkAttributesExtractor: CurrentNetworkAttributesExtractor = CurrentNetworkAttributesExtractor(),
) : LogRecordProcessor {
    constructor(networkProvider: CurrentNetworkProvider) : this(networkProvider, CurrentNetworkAttributesExtractor())

    override fun onEmit(
        context: Context,
        logRecord: ReadWriteLogRecord,
    ) {
        val currentNetwork = currentNetworkProvider.currentNetwork
        logRecord.setAllAttributes(networkAttributesExtractor.extract(currentNetwork))
    }
}
