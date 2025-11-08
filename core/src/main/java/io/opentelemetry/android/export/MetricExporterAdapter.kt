/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.sdk.metrics.export.MetricExporter

/**
 * Adapter for metric exporters that injects session identifiers into metric data.
 *
 * This adapter follows the Adapter design pattern to wrap a [MetricExporter] and
 * add session.id and session.previous_id attributes to all metric data points during export.
 * Session IDs are retrieved at export time to ensure they reflect the most current session state.
 */
internal interface MetricExporterAdapter : MetricExporter
