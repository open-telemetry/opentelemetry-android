/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export.models

import io.opentelemetry.android.export.factories.SessionMetricDataTypeFactory
import io.opentelemetry.sdk.metrics.data.SummaryData
import io.opentelemetry.sdk.metrics.data.SummaryPointData

/**
 * A [SummaryData] implementation that wraps another [SummaryData] and provides points with
 * session attributes injected.
 *
 * This class provides [SummaryPointData] with session attributes merged in. Summary data provides
 * statistical aggregates (count, sum, quantiles) and this wrapper preserves those aggregates while
 * adding session correlation.
 *
 * @property pointsWithSession the collection of SummaryPointData with session attributes injected.
 * @see SessionMetricDataTypeFactory
 */
internal class SummaryWithSessionData(
    private val pointsWithSession: Collection<SummaryPointData>,
) : SummaryData {
    override fun getPoints(): Collection<SummaryPointData> = pointsWithSession
}
