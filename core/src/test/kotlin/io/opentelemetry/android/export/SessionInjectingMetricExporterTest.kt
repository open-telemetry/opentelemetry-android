/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.export.factories.MetricDataFactory
import io.opentelemetry.android.export.factories.PointDataFactory
import io.opentelemetry.android.export.factories.SessionMetricDataFactory
import io.opentelemetry.android.export.factories.SessionMetricDataTypeFactory
import io.opentelemetry.android.export.factories.SessionMetricExporterAdapterFactory
import io.opentelemetry.android.export.factories.SessionPointDataFactory
import io.opentelemetry.android.session.SessionIdentifiers
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.common.InstrumentationScopeInfo
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData
import io.opentelemetry.sdk.metrics.data.DoublePointData
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramBuckets
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramData
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData
import io.opentelemetry.sdk.metrics.data.GaugeData
import io.opentelemetry.sdk.metrics.data.HistogramData
import io.opentelemetry.sdk.metrics.data.HistogramPointData
import io.opentelemetry.sdk.metrics.data.LongExemplarData
import io.opentelemetry.sdk.metrics.data.LongPointData
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.data.MetricDataType
import io.opentelemetry.sdk.metrics.data.SumData
import io.opentelemetry.sdk.metrics.data.SummaryData
import io.opentelemetry.sdk.metrics.data.SummaryPointData
import io.opentelemetry.sdk.metrics.data.ValueAtQuantile
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.resources.Resource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

private const val CURRENT_SESSION_ID = "current-session"
private const val HISTOGRAM_SESSION_ID = "histogram-session"
private const val METRIC_COUNT_FIVE = 5L
private const val METRIC_NAME_COUNTER = "test.counter"
private const val METRIC_NAME_SUM = "test.sum"
private const val METRIC_VALUE_FORTY_TWO = 42L
private const val METRIC_VALUE_HUNDRED = 100L
private const val METRIC_VALUE_TWO = 2L
private const val PREVIOUS_SESSION_ID = "previous-session"
private const val TEST_SESSION_123 = "test-session-123"
private const val TEST_SESSION_ID = "test-session"

/**
 * Verifies [SessionMetricExporterAdapter] injects session identifiers into all metric data types
 * and validates factory-based metric data transformation.
 */
@Suppress("LargeClass")
class SessionInjectingMetricExporterTest {
    private lateinit var delegate: MetricExporter
    private lateinit var sessionProvider: SessionProvider
    private lateinit var metricDataFactory: MetricDataFactory
    private lateinit var adapter: SessionMetricExporterAdapter

    @BeforeEach
    fun setUp() {
        delegate = mockk(relaxed = true)
        sessionProvider = mockk()
        metricDataFactory = SessionMetricDataFactory()
        adapter = SessionMetricExporterAdapter(delegate, sessionProvider, metricDataFactory)
    }

    @Test
    fun `should add session ID to metric points`() {
        // Verifies that session IDs are automatically injected into metric points during export

        // Given
        every { sessionProvider.getSessionId() } returns TEST_SESSION_123
        every { sessionProvider.getPreviousSessionId() } returns ""
        val metricsCaptor = slot<Collection<MetricData>>()
        every { delegate.export(capture(metricsCaptor)) } returns CompletableResultCode.ofSuccess()
        val originalMetric = createTestMetric(METRIC_NAME_COUNTER, METRIC_VALUE_FORTY_TWO)

        // When
        adapter.export(listOf(originalMetric))

        // Then
        verify { delegate.export(any()) }
        val exportedMetrics = metricsCaptor.captured
        val metric = exportedMetrics.first()
        val points = (metric.data as SumData<*>).points

        assertAll(
            { assertThat(exportedMetrics).hasSize(1) },
            {
                points.forEach { point ->
                    val longPoint = point as LongPointData
                    assertThat(longPoint.attributes.get(SessionIdentifiers.SESSION_ID))
                        .isEqualTo(TEST_SESSION_123)
                }
            },
        )
    }

    @Test
    fun `should add both current and previous session IDs`() {
        // Verifies that both current and previous session IDs are injected when available

        // Given
        every { sessionProvider.getSessionId() } returns CURRENT_SESSION_ID
        every { sessionProvider.getPreviousSessionId() } returns PREVIOUS_SESSION_ID
        val metricsCaptor = slot<Collection<MetricData>>()
        every { delegate.export(capture(metricsCaptor)) } returns CompletableResultCode.ofSuccess()
        val originalMetric = createTestMetric(METRIC_NAME_COUNTER, METRIC_VALUE_HUNDRED)

        // When
        adapter.export(listOf(originalMetric))

        // Then
        val exportedMetrics = metricsCaptor.captured
        val metric = exportedMetrics.first()
        val points = (metric.data as SumData<*>).points

        points.forEach { point ->
            val longPoint = point as LongPointData
            assertAll(
                {
                    assertThat(longPoint.attributes.get(SessionIdentifiers.SESSION_ID))
                        .isEqualTo(CURRENT_SESSION_ID)
                },
                {
                    assertThat(longPoint.attributes.get(SessionIdentifiers.SESSION_PREVIOUS_ID))
                        .isEqualTo(PREVIOUS_SESSION_ID)
                },
            )
        }
    }

    @Test
    fun `should not add blank session IDs`() {
        // Verifies that blank session IDs are omitted from metric attributes

        // Given
        every { sessionProvider.getSessionId() } returns ""
        every { sessionProvider.getPreviousSessionId() } returns ""
        val metricsCaptor = slot<Collection<MetricData>>()
        every { delegate.export(capture(metricsCaptor)) } returns CompletableResultCode.ofSuccess()
        val originalMetric = createTestMetric(METRIC_NAME_COUNTER, METRIC_COUNT_FIVE)

        // When
        adapter.export(listOf(originalMetric))

        // Then
        val exportedMetrics = metricsCaptor.captured
        val metric = exportedMetrics.first()
        val points = (metric.data as SumData<*>).points

        points.forEach { point ->
            val longPoint = point as LongPointData
            assertAll(
                { assertThat(longPoint.attributes.get(SessionIdentifiers.SESSION_ID)).isNull() },
                { assertThat(longPoint.attributes.get(SessionIdentifiers.SESSION_PREVIOUS_ID)).isNull() },
            )
        }
    }

    @Test
    fun `should preserve original metric attributes`() {
        // Verifies that original metric attributes are retained when session IDs are injected

        // Given
        val sessionId = "session-123"
        val envKey = "env"
        val envValue = "production"
        val versionKey = "version"
        val versionValue = 2L
        val metricValue = 10L

        every { sessionProvider.getSessionId() } returns sessionId
        every { sessionProvider.getPreviousSessionId() } returns ""
        val metricsCaptor = slot<Collection<MetricData>>()
        every { delegate.export(capture(metricsCaptor)) } returns CompletableResultCode.ofSuccess()

        val originalAttributes =
            Attributes.of(
                AttributeKey.stringKey(envKey),
                envValue,
                AttributeKey.longKey(versionKey),
                versionValue,
            )
        val originalMetric = createTestMetric(METRIC_NAME_COUNTER, metricValue, originalAttributes)

        // When
        adapter.export(listOf(originalMetric))

        // Then
        val exportedMetrics = metricsCaptor.captured
        val metric = exportedMetrics.first()
        val points = (metric.data as SumData<*>).points

        points.forEach { point ->
            val longPoint = point as LongPointData
            assertAll(
                {
                    assertThat(longPoint.attributes.get(AttributeKey.stringKey(envKey)))
                        .isEqualTo(envValue)
                },
                {
                    assertThat(longPoint.attributes.get(AttributeKey.longKey(versionKey)))
                        .isEqualTo(versionValue)
                },
                {
                    assertThat(longPoint.attributes.get(SessionIdentifiers.SESSION_ID))
                        .isEqualTo(sessionId)
                },
            )
        }
    }

    @Test
    fun `should handle multiple metrics in batch`() {
        // Verifies that session IDs are injected consistently across multiple metrics in a single export

        // Given
        val batchSessionId = "batch-session"
        val metricNameOne = "metric.one"
        val metricNameTwo = "metric.two"
        val metricNameThree = "metric.three"
        val expectedMetricsCount = 3

        every { sessionProvider.getSessionId() } returns batchSessionId
        every { sessionProvider.getPreviousSessionId() } returns ""
        val metricsCaptor = slot<Collection<MetricData>>()
        every { delegate.export(capture(metricsCaptor)) } returns CompletableResultCode.ofSuccess()

        val metrics =
            listOf(
                createTestMetric(metricNameOne, 1L),
                createTestMetric(metricNameTwo, METRIC_VALUE_TWO),
                createTestMetric(metricNameThree, 3L),
            )

        // When
        adapter.export(metrics)

        // Then
        val exportedMetrics = metricsCaptor.captured
        assertAll(
            { assertThat(exportedMetrics).hasSize(expectedMetricsCount) },
            {
                exportedMetrics.forEach { metric ->
                    val points = (metric.data as SumData<*>).points
                    points.forEach { point ->
                        val longPoint = point as LongPointData
                        assertThat(longPoint.attributes.get(SessionIdentifiers.SESSION_ID))
                            .isEqualTo(batchSessionId)
                    }
                }
            },
        )
    }

    @Test
    fun `should preserve metric metadata`() {
        // Verifies that metric metadata remains unchanged during session injection

        // Given
        val sessionId = "session"
        val metricName = "requests.total"
        val metricValue = 999L
        val expectedDescription = "Test metric description"
        val expectedUnit = "items"

        every { sessionProvider.getSessionId() } returns sessionId
        every { sessionProvider.getPreviousSessionId() } returns ""
        val metricsCaptor = slot<Collection<MetricData>>()
        every { delegate.export(capture(metricsCaptor)) } returns CompletableResultCode.ofSuccess()
        val originalMetric = createTestMetric(metricName, metricValue)

        // When
        adapter.export(listOf(originalMetric))

        // Then
        val exportedMetrics = metricsCaptor.captured
        val metric = exportedMetrics.first()

        assertAll(
            { assertThat(metric.name).isEqualTo(metricName) },
            { assertThat(metric.description).isEqualTo(expectedDescription) },
            { assertThat(metric.unit).isEqualTo(expectedUnit) },
            { assertThat(metric.resource).isEqualTo(Resource.getDefault()) },
        )
    }

    @Test
    fun `should delegate flush to underlying exporter`() {
        // Verifies that flush operations are correctly delegated to the wrapped exporter

        // Given
        val expectedResult = CompletableResultCode.ofSuccess()
        every { delegate.flush() } returns expectedResult

        // When
        val result = adapter.flush()

        // Then
        assertAll(
            { assertThat(result).isEqualTo(expectedResult) },
            { verify { delegate.flush() } },
        )
    }

    @Test
    fun `should delegate shutdown to underlying exporter`() {
        // Verifies that shutdown operations are correctly delegated to the wrapped exporter

        // Given
        val expectedResult = CompletableResultCode.ofSuccess()
        every { delegate.shutdown() } returns expectedResult

        // When
        val result = adapter.shutdown()

        // Then
        assertAll(
            { assertThat(result).isEqualTo(expectedResult) },
            { verify { delegate.shutdown() } },
        )
    }

    @Test
    fun `should delegate aggregation temporality`() {
        // Verifies that aggregation temporality queries are correctly delegated

        // Given
        val instrumentType = InstrumentType.COUNTER
        every { delegate.getAggregationTemporality(instrumentType) } returns AggregationTemporality.DELTA

        // When
        val result = adapter.getAggregationTemporality(instrumentType)

        // Then
        assertAll(
            { assertThat(result).isEqualTo(AggregationTemporality.DELTA) },
            { verify { delegate.getAggregationTemporality(instrumentType) } },
        )
    }

    @Test
    fun `exporter adapter factory should create SessionMetricExporterAdapter`() {
        // Verifies that the factory correctly creates adapter instances

        // Given
        val factory = SessionMetricExporterAdapterFactory()

        // When
        val createdAdapter = factory.createMetricExporterAdapter(delegate, sessionProvider)

        // Then
        assertAll(
            { assertThat(createdAdapter).isInstanceOf(MetricExporterAdapter::class.java) },
            { assertThat(createdAdapter).isInstanceOf(SessionMetricExporterAdapter::class.java) },
        )
    }

    @Test
    fun `metric data factory should create MetricData with injected attributes`() {
        // Verifies that metric data factory correctly creates modified metric data with session attributes

        // Given
        val factory = SessionMetricDataFactory()
        val originalMetric = createTestMetric(METRIC_NAME_COUNTER, METRIC_VALUE_HUNDRED)
        val sessionIdKey = "session.id"
        val sessionIdValue = TEST_SESSION_ID
        val sessionAttributes =
            Attributes
                .builder()
                .put(sessionIdKey, sessionIdValue)
                .build()

        // When
        val modifiedMetric = factory.createMetricDataWithAttributes(originalMetric, sessionAttributes)

        // Then
        val points = (modifiedMetric.data as SumData<*>).points
        assertAll(
            { assertThat(modifiedMetric).isNotNull() },
            { assertThat(modifiedMetric.name).isEqualTo(METRIC_NAME_COUNTER) },
            {
                points.forEach { point ->
                    val longPoint = point as LongPointData
                    assertThat(longPoint.attributes.get(AttributeKey.stringKey(sessionIdKey)))
                        .isEqualTo(sessionIdValue)
                }
            },
        )
    }

    @Test
    fun `metric data type factory should create adapted SumData`() {
        // Verifies that data type factory correctly adapts SumData with session attributes

        // Given
        val factory = SessionMetricDataTypeFactory()
        val metricValue = 200L
        val originalMetric = createTestMetric(METRIC_NAME_COUNTER, metricValue)
        val sessionIdKey = "session.id"
        val sessionIdValue = "type-factory-session"
        val sessionAttributes =
            Attributes
                .builder()
                .put(sessionIdKey, sessionIdValue)
                .build()

        // When
        val adaptedData = factory.createDataWithAttributes(originalMetric.data, sessionAttributes)

        // Then
        val points = (adaptedData as SumData<*>).points
        assertAll(
            { assertThat(adaptedData).isInstanceOf(SumData::class.java) },
            {
                points.forEach { point ->
                    val longPoint = point as LongPointData
                    assertThat(longPoint.attributes.get(AttributeKey.stringKey(sessionIdKey)))
                        .isEqualTo(sessionIdValue)
                }
            },
        )
    }

    @Test
    fun `should not add previous session ID if blank`() {
        // Verifies that blank previous session IDs (including whitespace) are omitted

        // Given
        val blankPreviousSessionId = "   "
        val metricName = "test.metric"
        val metricValue = 50L

        every { sessionProvider.getSessionId() } returns CURRENT_SESSION_ID
        every { sessionProvider.getPreviousSessionId() } returns blankPreviousSessionId
        val metricsCaptor = slot<Collection<MetricData>>()
        every { delegate.export(capture(metricsCaptor)) } returns CompletableResultCode.ofSuccess()
        val originalMetric = createTestMetric(metricName, metricValue)

        // When
        adapter.export(listOf(originalMetric))

        // Then
        val exportedMetrics = metricsCaptor.captured
        val metric = exportedMetrics.first()
        val points = (metric.data as SumData<*>).points

        points.forEach { point ->
            val longPoint = point as LongPointData
            assertAll(
                {
                    assertThat(longPoint.attributes.get(SessionIdentifiers.SESSION_ID))
                        .isEqualTo(CURRENT_SESSION_ID)
                },
                { assertThat(longPoint.attributes.get(SessionIdentifiers.SESSION_PREVIOUS_ID)).isNull() },
            )
        }
    }

    @Test
    fun `should handle GaugeData with DoublePointData`() {
        // Verifies that GaugeData with double values receives session IDs correctly

        // Given
        val gaugeSessionId = "gauge-session"
        val metricName = "test.gauge"
        val gaugeValue = 98.6

        every { sessionProvider.getSessionId() } returns gaugeSessionId
        every { sessionProvider.getPreviousSessionId() } returns ""
        val metricsCaptor = slot<Collection<MetricData>>()
        every { delegate.export(capture(metricsCaptor)) } returns CompletableResultCode.ofSuccess()
        val originalMetric = createGaugeMetric(metricName, gaugeValue)

        // When
        adapter.export(listOf(originalMetric))

        // Then
        val exportedMetrics = metricsCaptor.captured
        val metric = exportedMetrics.first()
        val points = (metric.data as GaugeData<*>).points

        points.forEach { point ->
            val doublePoint = point as DoublePointData
            assertAll(
                {
                    assertThat(doublePoint.attributes.get(SessionIdentifiers.SESSION_ID))
                        .isEqualTo(gaugeSessionId)
                },
                { assertThat(doublePoint.value).isEqualTo(gaugeValue) },
            )
        }
    }

    @Test
    fun `should handle HistogramData`() {
        // Verifies that HistogramData receives session IDs correctly

        // Given
        val metricName = "test.histogram"
        val expectedCount = 10L
        val expectedSum = 100.0

        every { sessionProvider.getSessionId() } returns HISTOGRAM_SESSION_ID
        every { sessionProvider.getPreviousSessionId() } returns ""
        val metricsCaptor = slot<Collection<MetricData>>()
        every { delegate.export(capture(metricsCaptor)) } returns CompletableResultCode.ofSuccess()
        val originalMetric = createHistogramMetric(metricName)

        // When
        adapter.export(listOf(originalMetric))

        // Then
        val exportedMetrics = metricsCaptor.captured
        val metric = exportedMetrics.first()
        val points = (metric.data as HistogramData).points

        points.forEach { point ->
            assertAll(
                {
                    assertThat(point.attributes.get(SessionIdentifiers.SESSION_ID))
                        .isEqualTo(HISTOGRAM_SESSION_ID)
                },
                { assertThat(point.count).isEqualTo(expectedCount) },
                { assertThat(point.sum).isEqualTo(expectedSum) },
            )
        }
    }

    @Test
    fun `should handle ExponentialHistogramData`() {
        // Verifies that ExponentialHistogramData receives session IDs correctly

        // Given
        val expHistogramSessionId = "exp-histogram-session"
        val metricName = "test.exp.histogram"
        val expectedScale = 2
        val expectedZeroCount = 5L

        every { sessionProvider.getSessionId() } returns expHistogramSessionId
        every { sessionProvider.getPreviousSessionId() } returns ""
        val metricsCaptor = slot<Collection<MetricData>>()
        every { delegate.export(capture(metricsCaptor)) } returns CompletableResultCode.ofSuccess()
        val originalMetric = createExponentialHistogramMetric(metricName)

        // When
        adapter.export(listOf(originalMetric))

        // Then
        val exportedMetrics = metricsCaptor.captured
        val metric = exportedMetrics.first()
        val points = (metric.data as ExponentialHistogramData).points

        points.forEach { point ->
            assertAll(
                {
                    assertThat(point.attributes.get(SessionIdentifiers.SESSION_ID))
                        .isEqualTo(expHistogramSessionId)
                },
                { assertThat(point.scale).isEqualTo(expectedScale) },
                { assertThat(point.zeroCount).isEqualTo(expectedZeroCount) },
            )
        }
    }

    @Test
    fun `should handle SummaryData`() {
        // Verifies that SummaryData receives session IDs correctly

        // Given
        val summarySessionId = "summary-session"
        val metricName = "test.summary"
        val expectedCount = 20L
        val expectedSum = 200.0

        every { sessionProvider.getSessionId() } returns summarySessionId
        every { sessionProvider.getPreviousSessionId() } returns ""
        val metricsCaptor = slot<Collection<MetricData>>()
        every { delegate.export(capture(metricsCaptor)) } returns CompletableResultCode.ofSuccess()
        val originalMetric = createSummaryMetric(metricName)

        // When
        adapter.export(listOf(originalMetric))

        // Then
        val exportedMetrics = metricsCaptor.captured
        val metric = exportedMetrics.first()
        val points = (metric.data as SummaryData).points

        points.forEach { point ->
            assertAll(
                {
                    assertThat(point.attributes.get(SessionIdentifiers.SESSION_ID))
                        .isEqualTo(summarySessionId)
                },
                { assertThat(point.count).isEqualTo(expectedCount) },
                { assertThat(point.sum).isEqualTo(expectedSum) },
            )
        }
    }

    @Test
    @Suppress("LongMethod")
    fun `PointDataFactory should handle all point types`() {
        // Verifies that PointDataFactory correctly adapts all supported point data types

        // Given
        val factory = SessionPointDataFactory()
        val sessionIdKey = "session.id"
        val sessionIdValue = "point-test"
        val sessionAttributes = Attributes.builder().put(sessionIdKey, sessionIdValue).build()
        val startTimeNanos = 1000L
        val endTimeNanos = 2000L
        val piValue = 3.14

        // When/Then: Test LongPointData
        val longPoint =
            mockk<LongPointData> {
                every { getStartEpochNanos() } returns startTimeNanos
                every { getEpochNanos() } returns endTimeNanos
                every { getAttributes() } returns Attributes.empty()
                every { getValue() } returns METRIC_VALUE_FORTY_TWO
                every { getExemplars() } returns emptyList()
            }
        val modifiedLongPoint = factory.createPointDataWithAttributes(longPoint, sessionAttributes) as LongPointData
        assertAll(
            { assertThat(modifiedLongPoint.attributes.get(AttributeKey.stringKey(sessionIdKey))).isEqualTo(sessionIdValue) },
            { assertThat(modifiedLongPoint.value).isEqualTo(METRIC_VALUE_FORTY_TWO) },
        )

        // When/Then: Test DoublePointData
        val doublePoint =
            mockk<DoublePointData> {
                every { getStartEpochNanos() } returns startTimeNanos
                every { getEpochNanos() } returns endTimeNanos
                every { getAttributes() } returns Attributes.empty()
                every { getValue() } returns piValue
                every { getExemplars() } returns emptyList()
            }
        val modifiedDoublePoint = factory.createPointDataWithAttributes(doublePoint, sessionAttributes) as DoublePointData
        assertAll(
            { assertThat(modifiedDoublePoint.attributes.get(AttributeKey.stringKey(sessionIdKey))).isEqualTo(sessionIdValue) },
            { assertThat(modifiedDoublePoint.value).isEqualTo(piValue) },
        )

        // When/Then: Test HistogramPointData
        val histogramCount = 5L
        val histogramPoint =
            mockk<HistogramPointData> {
                every { getStartEpochNanos() } returns startTimeNanos
                every { getEpochNanos() } returns endTimeNanos
                every { getAttributes() } returns Attributes.empty()
                every { getCount() } returns histogramCount
                every { getSum() } returns 50.0
                every { getBoundaries() } returns listOf(1.0, 10.0, 100.0)
                every { getCounts() } returns listOf(1L, 2L, 2L, 0L)
                every { getExemplars() } returns emptyList()
            }
        val modifiedHistogramPoint = factory.createPointDataWithAttributes(histogramPoint, sessionAttributes) as HistogramPointData
        assertAll(
            { assertThat(modifiedHistogramPoint.attributes.get(AttributeKey.stringKey(sessionIdKey))).isEqualTo(sessionIdValue) },
            { assertThat(modifiedHistogramPoint.count).isEqualTo(histogramCount) },
        )

        // When/Then: Test ExponentialHistogramPointData
        val expHistogramCount = 10L
        val expHistogramScale = 2
        val expHistogramPoint =
            mockk<ExponentialHistogramPointData> {
                every { getStartEpochNanos() } returns startTimeNanos
                every { getEpochNanos() } returns endTimeNanos
                every { getAttributes() } returns Attributes.empty()
                every { getCount() } returns expHistogramCount
                every { getSum() } returns 100.0
                every { getScale() } returns expHistogramScale
                every { getZeroCount() } returns 3L
                every { getPositiveBuckets() } returns mockk<ExponentialHistogramBuckets>(relaxed = true)
                every { getNegativeBuckets() } returns mockk<ExponentialHistogramBuckets>(relaxed = true)
                every { getExemplars() } returns emptyList()
            }
        val modifiedExpHistogramPoint =
            factory.createPointDataWithAttributes(
                expHistogramPoint,
                sessionAttributes,
            ) as ExponentialHistogramPointData
        assertAll(
            { assertThat(modifiedExpHistogramPoint.attributes.get(AttributeKey.stringKey(sessionIdKey))).isEqualTo(sessionIdValue) },
            { assertThat(modifiedExpHistogramPoint.scale).isEqualTo(expHistogramScale) },
        )

        // When/Then: Test SummaryPointData
        val summaryCount = 15L
        val summaryPoint =
            mockk<SummaryPointData> {
                every { getStartEpochNanos() } returns startTimeNanos
                every { getEpochNanos() } returns endTimeNanos
                every { getAttributes() } returns Attributes.empty()
                every { getCount() } returns summaryCount
                every { getSum() } returns 150.0
                every { getValues() } returns listOf(mockk<ValueAtQuantile>(relaxed = true))
                every { getExemplars() } returns emptyList()
            }
        val modifiedSummaryPoint = factory.createPointDataWithAttributes(summaryPoint, sessionAttributes) as SummaryPointData
        assertAll(
            { assertThat(modifiedSummaryPoint.attributes.get(AttributeKey.stringKey(sessionIdKey))).isEqualTo(sessionIdValue) },
            { assertThat(modifiedSummaryPoint.count).isEqualTo(summaryCount) },
        )
    }

    @Test
    fun `MetricDataTypeFactory should handle unknown data types`() {
        // Verifies that factory gracefully handles unknown data types by returning original data

        // Given
        val factory = SessionMetricDataTypeFactory()
        val unknownData = mockk<io.opentelemetry.sdk.metrics.data.Data<*>>()
        val sessionIdKey = "session.id"
        val sessionIdValue = TEST_SESSION_ID
        val sessionAttributes = Attributes.builder().put(sessionIdKey, sessionIdValue).build()

        // When
        val result = factory.createDataWithAttributes(unknownData, sessionAttributes)

        // Then
        assertThat(result).isSameAs(unknownData)
    }

    @Test
    fun `PointDataFactory should handle unknown point types`() {
        // Verifies that factory gracefully handles unknown point types by returning original point

        // Given
        val factory = SessionPointDataFactory()
        val unknownPoint = mockk<io.opentelemetry.sdk.metrics.data.PointData>()
        val sessionIdKey = "session.id"
        val sessionIdValue = TEST_SESSION_ID
        val sessionAttributes = Attributes.builder().put(sessionIdKey, sessionIdValue).build()

        // When
        val result = factory.createPointDataWithAttributes(unknownPoint, sessionAttributes)

        // Then
        assertThat(result).isSameAs(unknownPoint)
    }

    @Test
    fun `ModifiedMetricData should preserve all metadata`() {
        // Verifies that all metric metadata is preserved during session attribute injection

        // Given
        val factory = SessionMetricDataFactory()
        val metricName = "test.metric"
        val metricValue = 123L
        val testAttrKey = "test.attr"
        val testAttrValue = "value"
        val expectedDescription = "Test metric description"
        val expectedUnit = "items"
        val expectedScopeName = "test.scope"

        val originalMetric = createTestMetric(metricName, metricValue)
        val sessionAttributes = Attributes.builder().put(testAttrKey, testAttrValue).build()

        // When
        val modifiedMetric = factory.createMetricDataWithAttributes(originalMetric, sessionAttributes)

        // Then
        val data = modifiedMetric.data
        assertAll(
            { assertThat(modifiedMetric.name).isEqualTo(metricName) },
            { assertThat(modifiedMetric.description).isEqualTo(expectedDescription) },
            { assertThat(modifiedMetric.unit).isEqualTo(expectedUnit) },
            { assertThat(modifiedMetric.type).isEqualTo(MetricDataType.LONG_SUM) },
            { assertThat(modifiedMetric.resource).isEqualTo(Resource.getDefault()) },
            { assertThat(modifiedMetric.instrumentationScopeInfo.name).isEqualTo(expectedScopeName) },
            { assertThat(modifiedMetric.isEmpty).isFalse() },
            { assertThat(data).isInstanceOf(SumData::class.java) },
        )
    }

    @Test
    fun `SumWithSessionData should preserve monotonic and aggregation temporality`() {
        // Verifies that SumData properties are preserved during session injection

        // Given
        val factory = SessionMetricDataTypeFactory()
        val originalMetric = createTestMetric(METRIC_NAME_SUM, METRIC_VALUE_HUNDRED)
        val sessionIdKey = "session.id"
        val sessionIdValue = TEST_SESSION_ID
        val sessionAttributes = Attributes.builder().put(sessionIdKey, sessionIdValue).build()

        // When
        val adaptedData = factory.createDataWithAttributes(originalMetric.data, sessionAttributes) as SumData<*>

        // Then
        assertAll(
            { assertThat(adaptedData.isMonotonic).isTrue() },
            { assertThat(adaptedData.aggregationTemporality).isEqualTo(AggregationTemporality.CUMULATIVE) },
        )
    }

    @Test
    fun `HistogramWithSessionData should preserve aggregation temporality`() {
        // Verifies that HistogramData aggregation temporality is preserved

        // Given
        val factory = SessionMetricDataTypeFactory()
        val metricName = "test.histogram"
        val originalMetric = createHistogramMetric(metricName)
        val sessionIdKey = "session.id"
        val sessionIdValue = TEST_SESSION_ID
        val sessionAttributes = Attributes.builder().put(sessionIdKey, sessionIdValue).build()

        // When
        val adaptedData = factory.createDataWithAttributes(originalMetric.data, sessionAttributes) as HistogramData

        // Then
        assertThat(adaptedData.aggregationTemporality).isEqualTo(AggregationTemporality.DELTA)
    }

    @Test
    fun `ExponentialHistogramWithSessionData should preserve aggregation temporality`() {
        // Verifies that ExponentialHistogramData aggregation temporality is preserved

        // Given
        val factory = SessionMetricDataTypeFactory()
        val metricName = "test.exp.histogram"
        val originalMetric = createExponentialHistogramMetric(metricName)
        val sessionIdKey = "session.id"
        val sessionIdValue = TEST_SESSION_ID
        val sessionAttributes = Attributes.builder().put(sessionIdKey, sessionIdValue).build()

        // When
        val adaptedData = factory.createDataWithAttributes(originalMetric.data, sessionAttributes) as ExponentialHistogramData

        // Then
        assertThat(adaptedData.aggregationTemporality).isEqualTo(AggregationTemporality.CUMULATIVE)
    }

    @Test
    fun `LongPointWithSessionData should preserve exemplars`() {
        // Verifies that exemplar data is preserved for long point data

        // Given
        val factory: PointDataFactory = SessionPointDataFactory()
        val exemplar = mockk<LongExemplarData>(relaxed = true)
        val startTimeNanos = 1000L
        val endTimeNanos = 2000L
        val longPoint =
            mockk<LongPointData> {
                every { getStartEpochNanos() } returns startTimeNanos
                every { getEpochNanos() } returns endTimeNanos
                every { getAttributes() } returns Attributes.empty()
                every { getValue() } returns METRIC_VALUE_FORTY_TWO
                every { getExemplars() } returns listOf(exemplar)
            }

        // When
        val modifiedPoint = factory.createPointDataWithAttributes(longPoint, Attributes.empty()) as LongPointData

        // Then
        assertAll(
            { assertThat(modifiedPoint.exemplars).hasSize(1) },
            { assertThat(modifiedPoint.exemplars[0]).isSameAs(exemplar) },
        )
    }

    @Test
    fun `DoublePointWithSessionData should preserve exemplars`() {
        // Verifies that exemplar data is preserved for double point data

        // Given
        val factory: PointDataFactory = SessionPointDataFactory()
        val exemplar = mockk<DoubleExemplarData>(relaxed = true)
        val startTimeNanos = 1000L
        val endTimeNanos = 2000L
        val piValue = 3.14
        val doublePoint =
            mockk<DoublePointData> {
                every { getStartEpochNanos() } returns startTimeNanos
                every { getEpochNanos() } returns endTimeNanos
                every { getAttributes() } returns Attributes.empty()
                every { getValue() } returns piValue
                every { getExemplars() } returns listOf(exemplar)
            }

        // When
        val modifiedPoint = factory.createPointDataWithAttributes(doublePoint, Attributes.empty()) as DoublePointData

        // Then
        assertAll(
            { assertThat(modifiedPoint.exemplars).hasSize(1) },
            { assertThat(modifiedPoint.exemplars[0]).isSameAs(exemplar) },
        )
    }

    @Test
    fun `HistogramPointWithSessionData should preserve all histogram properties`() {
        // Verifies that all histogram point properties are preserved

        // Given
        val factory: PointDataFactory = SessionPointDataFactory()
        val startTimeNanos = 1000L
        val endTimeNanos = 2000L
        val count = 10L
        val sum = 123.45
        val boundaries = listOf(1.0, 10.0, 100.0)
        val counts = listOf(1L, 2L, 3L, 4L)

        val histogramPoint =
            mockk<HistogramPointData> {
                every { getStartEpochNanos() } returns startTimeNanos
                every { getEpochNanos() } returns endTimeNanos
                every { getAttributes() } returns Attributes.empty()
                every { getCount() } returns count
                every { getSum() } returns sum
                every { getBoundaries() } returns boundaries
                every { getCounts() } returns counts
                every { getExemplars() } returns emptyList()
            }

        // When
        val modifiedPoint = factory.createPointDataWithAttributes(histogramPoint, Attributes.empty()) as HistogramPointData

        // Then
        assertAll(
            { assertThat(modifiedPoint.startEpochNanos).isEqualTo(startTimeNanos) },
            { assertThat(modifiedPoint.epochNanos).isEqualTo(endTimeNanos) },
            { assertThat(modifiedPoint.count).isEqualTo(count) },
            { assertThat(modifiedPoint.sum).isEqualTo(sum) },
            { assertThat(modifiedPoint.boundaries).isEqualTo(boundaries) },
            { assertThat(modifiedPoint.counts).isEqualTo(counts) },
        )
    }

    @Test
    fun `ExponentialHistogramPointWithSessionData should preserve all exponential histogram properties`() {
        // Verifies that all exponential histogram point properties are preserved

        // Given
        val factory: PointDataFactory = SessionPointDataFactory()
        val startTimeNanos = 1000L
        val endTimeNanos = 2000L
        val count = 15L
        val sum = 234.56
        val scale = 3
        val zeroCount = 7L
        val positiveBuckets = mockk<ExponentialHistogramBuckets>(relaxed = true)
        val negativeBuckets = mockk<ExponentialHistogramBuckets>(relaxed = true)

        val expHistogramPoint =
            mockk<ExponentialHistogramPointData> {
                every { getStartEpochNanos() } returns startTimeNanos
                every { getEpochNanos() } returns endTimeNanos
                every { getAttributes() } returns Attributes.empty()
                every { getCount() } returns count
                every { getSum() } returns sum
                every { getScale() } returns scale
                every { getZeroCount() } returns zeroCount
                every { getPositiveBuckets() } returns positiveBuckets
                every { getNegativeBuckets() } returns negativeBuckets
                every { getExemplars() } returns emptyList()
            }

        // When
        val modifiedPoint = factory.createPointDataWithAttributes(expHistogramPoint, Attributes.empty()) as ExponentialHistogramPointData

        // Then
        assertAll(
            { assertThat(modifiedPoint.startEpochNanos).isEqualTo(startTimeNanos) },
            { assertThat(modifiedPoint.epochNanos).isEqualTo(endTimeNanos) },
            { assertThat(modifiedPoint.count).isEqualTo(count) },
            { assertThat(modifiedPoint.sum).isEqualTo(sum) },
            { assertThat(modifiedPoint.scale).isEqualTo(scale) },
            { assertThat(modifiedPoint.zeroCount).isEqualTo(zeroCount) },
            { assertThat(modifiedPoint.positiveBuckets).isSameAs(positiveBuckets) },
            { assertThat(modifiedPoint.negativeBuckets).isSameAs(negativeBuckets) },
        )
    }

    @Test
    fun `SummaryPointWithSessionData should preserve all summary properties`() {
        // Verifies that all summary point properties are preserved

        // Given
        val factory: PointDataFactory = SessionPointDataFactory()
        val startTimeNanos = 1000L
        val endTimeNanos = 2000L
        val count = 20L
        val sum = 345.67
        val values = listOf(mockk<ValueAtQuantile>(relaxed = true))
        val exemplars = listOf(mockk<DoubleExemplarData>(relaxed = true))

        val summaryPoint =
            mockk<SummaryPointData> {
                every { getStartEpochNanos() } returns startTimeNanos
                every { getEpochNanos() } returns endTimeNanos
                every { getAttributes() } returns Attributes.empty()
                every { getCount() } returns count
                every { getSum() } returns sum
                every { getValues() } returns values
                every { getExemplars() } returns exemplars
            }

        // When
        val modifiedPoint = factory.createPointDataWithAttributes(summaryPoint, Attributes.empty()) as SummaryPointData

        // Then
        assertAll(
            { assertThat(modifiedPoint.startEpochNanos).isEqualTo(startTimeNanos) },
            { assertThat(modifiedPoint.epochNanos).isEqualTo(endTimeNanos) },
            { assertThat(modifiedPoint.count).isEqualTo(count) },
            { assertThat(modifiedPoint.sum).isEqualTo(sum) },
            { assertThat(modifiedPoint.values).isEqualTo(values) },
            { assertThat(modifiedPoint.exemplars).hasSize(1) },
        )
    }

    @Test
    fun `HistogramPointWithSessionData should preserve min and max when present`() {
        // Verifies that min and max values are preserved when present in histogram points

        // Given
        val factory: PointDataFactory = SessionPointDataFactory()
        val startTimeNanos = 1000L
        val endTimeNanos = 2000L
        val count = 10L
        val sum = 123.45
        val minValue = 1.5
        val maxValue = 50.0
        val boundariesSize = 3
        val countsSize = 4

        val histogramPoint =
            mockk<HistogramPointData> {
                every { getStartEpochNanos() } returns startTimeNanos
                every { getEpochNanos() } returns endTimeNanos
                every { getAttributes() } returns Attributes.empty()
                every { getCount() } returns count
                every { getSum() } returns sum
                every { hasMin() } returns true
                every { getMin() } returns minValue
                every { hasMax() } returns true
                every { getMax() } returns maxValue
                every { getBoundaries() } returns listOf(1.0, 10.0, 100.0)
                every { getCounts() } returns listOf(2L, 5L, 3L, 0L)
                every { getExemplars() } returns emptyList()
            }

        // When
        val modifiedPoint = factory.createPointDataWithAttributes(histogramPoint, Attributes.empty()) as HistogramPointData

        // Then
        assertAll(
            { assertThat(modifiedPoint.startEpochNanos).isEqualTo(startTimeNanos) },
            { assertThat(modifiedPoint.epochNanos).isEqualTo(endTimeNanos) },
            { assertThat(modifiedPoint.count).isEqualTo(count) },
            { assertThat(modifiedPoint.sum).isEqualTo(sum) },
            { assertThat(modifiedPoint.hasMin()).isTrue() },
            { assertThat(modifiedPoint.min).isEqualTo(minValue) },
            { assertThat(modifiedPoint.hasMax()).isTrue() },
            { assertThat(modifiedPoint.max).isEqualTo(maxValue) },
            { assertThat(modifiedPoint.boundaries).hasSize(boundariesSize) },
            { assertThat(modifiedPoint.counts).hasSize(countsSize) },
        )
    }

    @Test
    fun `HistogramPointWithSessionData should handle no min and max`() {
        // Verifies that histogram points without min/max values are handled correctly

        // Given
        val factory: PointDataFactory = SessionPointDataFactory()
        val startTimeNanos = 1000L
        val endTimeNanos = 2000L
        val count = 5L
        val sum = 50.0

        val histogramPoint =
            mockk<HistogramPointData> {
                every { getStartEpochNanos() } returns startTimeNanos
                every { getEpochNanos() } returns endTimeNanos
                every { getAttributes() } returns Attributes.empty()
                every { getCount() } returns count
                every { getSum() } returns sum
                every { hasMin() } returns false
                every { getMin() } returns Double.NaN
                every { hasMax() } returns false
                every { getMax() } returns Double.NaN
                every { getBoundaries() } returns emptyList()
                every { getCounts() } returns emptyList()
                every { getExemplars() } returns emptyList()
            }

        // When
        val modifiedPoint = factory.createPointDataWithAttributes(histogramPoint, Attributes.empty()) as HistogramPointData

        // Then
        assertAll(
            { assertThat(modifiedPoint.hasMin()).isFalse() },
            { assertThat(modifiedPoint.min).isNaN() },
            { assertThat(modifiedPoint.hasMax()).isFalse() },
            { assertThat(modifiedPoint.max).isNaN() },
        )
    }

    @Test
    fun `ExponentialHistogramPointWithSessionData should preserve min and max when present`() {
        // Verifies that min and max values are preserved when present in exponential histogram points

        // Given
        val factory: PointDataFactory = SessionPointDataFactory()
        val startTimeNanos = 1000L
        val endTimeNanos = 2000L
        val count = 15L
        val sum = 234.56
        val scale = 3
        val zeroCount = 7L
        val minValue = 0.5
        val maxValue = 100.0
        val positiveBuckets = mockk<ExponentialHistogramBuckets>(relaxed = true)
        val negativeBuckets = mockk<ExponentialHistogramBuckets>(relaxed = true)

        val expHistogramPoint =
            mockk<ExponentialHistogramPointData> {
                every { getStartEpochNanos() } returns startTimeNanos
                every { getEpochNanos() } returns endTimeNanos
                every { getAttributes() } returns Attributes.empty()
                every { getCount() } returns count
                every { getSum() } returns sum
                every { getScale() } returns scale
                every { getZeroCount() } returns zeroCount
                every { hasMin() } returns true
                every { getMin() } returns minValue
                every { hasMax() } returns true
                every { getMax() } returns maxValue
                every { getPositiveBuckets() } returns positiveBuckets
                every { getNegativeBuckets() } returns negativeBuckets
                every { getExemplars() } returns emptyList()
            }

        // When
        val modifiedPoint = factory.createPointDataWithAttributes(expHistogramPoint, Attributes.empty()) as ExponentialHistogramPointData

        // Then
        assertAll(
            { assertThat(modifiedPoint.startEpochNanos).isEqualTo(startTimeNanos) },
            { assertThat(modifiedPoint.epochNanos).isEqualTo(endTimeNanos) },
            { assertThat(modifiedPoint.count).isEqualTo(count) },
            { assertThat(modifiedPoint.sum).isEqualTo(sum) },
            { assertThat(modifiedPoint.scale).isEqualTo(scale) },
            { assertThat(modifiedPoint.zeroCount).isEqualTo(zeroCount) },
            { assertThat(modifiedPoint.hasMin()).isTrue() },
            { assertThat(modifiedPoint.min).isEqualTo(minValue) },
            { assertThat(modifiedPoint.hasMax()).isTrue() },
            { assertThat(modifiedPoint.max).isEqualTo(maxValue) },
            { assertThat(modifiedPoint.positiveBuckets).isSameAs(positiveBuckets) },
            { assertThat(modifiedPoint.negativeBuckets).isSameAs(negativeBuckets) },
        )
    }

    @Test
    fun `ExponentialHistogramPointWithSessionData should handle no min and max`() {
        // Verifies that exponential histogram points without min/max values are handled correctly

        // Given
        val factory: PointDataFactory = SessionPointDataFactory()
        val startTimeNanos = 1000L
        val endTimeNanos = 2000L
        val count = 10L
        val sum = 100.0
        val scale = 2
        val zeroCount = 3L

        val expHistogramPoint =
            mockk<ExponentialHistogramPointData> {
                every { getStartEpochNanos() } returns startTimeNanos
                every { getEpochNanos() } returns endTimeNanos
                every { getAttributes() } returns Attributes.empty()
                every { getCount() } returns count
                every { getSum() } returns sum
                every { getScale() } returns scale
                every { getZeroCount() } returns zeroCount
                every { hasMin() } returns false
                every { getMin() } returns Double.NaN
                every { hasMax() } returns false
                every { getMax() } returns Double.NaN
                every { getPositiveBuckets() } returns mockk<ExponentialHistogramBuckets>(relaxed = true)
                every { getNegativeBuckets() } returns mockk<ExponentialHistogramBuckets>(relaxed = true)
                every { getExemplars() } returns emptyList()
            }

        // When
        val modifiedPoint = factory.createPointDataWithAttributes(expHistogramPoint, Attributes.empty()) as ExponentialHistogramPointData

        // Then
        assertAll(
            { assertThat(modifiedPoint.hasMin()).isFalse() },
            { assertThat(modifiedPoint.min).isNaN() },
            { assertThat(modifiedPoint.hasMax()).isFalse() },
            { assertThat(modifiedPoint.max).isNaN() },
        )
    }

    private fun createTestMetric(
        name: String,
        value: Long,
        attributes: Attributes = Attributes.empty(),
    ): MetricData {
        val point =
            mockk<LongPointData> {
                every { getStartEpochNanos() } returns 1000000000L
                every { getEpochNanos() } returns 2000000000L
                every { getAttributes() } returns attributes
                every { getValue() } returns value
                every { getExemplars() } returns emptyList()
            }

        val sumData =
            mockk<SumData<LongPointData>> {
                every { getPoints() } returns listOf(point)
                every { isMonotonic() } returns true
                every { getAggregationTemporality() } returns AggregationTemporality.CUMULATIVE
            }

        return mockk<MetricData> {
            every { getName() } returns name
            every { getDescription() } returns "Test metric description"
            every { getUnit() } returns "items"
            every { getType() } returns MetricDataType.LONG_SUM
            every { getResource() } returns Resource.getDefault()
            every { getInstrumentationScopeInfo() } returns InstrumentationScopeInfo.create("test.scope")
            every { getData() } returns sumData
            every { isEmpty() } returns false
        }
    }

    private fun createGaugeMetric(
        name: String,
        value: Double,
        attributes: Attributes = Attributes.empty(),
    ): MetricData {
        val point =
            mockk<DoublePointData> {
                every { getStartEpochNanos() } returns 1000000000L
                every { getEpochNanos() } returns 2000000000L
                every { getAttributes() } returns attributes
                every { getValue() } returns value
                every { getExemplars() } returns emptyList()
            }

        val gaugeData =
            mockk<GaugeData<DoublePointData>> {
                every { getPoints() } returns listOf(point)
            }

        return mockk<MetricData> {
            every { getName() } returns name
            every { getDescription() } returns "Test gauge description"
            every { getUnit() } returns "celsius"
            every { getType() } returns MetricDataType.DOUBLE_GAUGE
            every { getResource() } returns Resource.getDefault()
            every { getInstrumentationScopeInfo() } returns InstrumentationScopeInfo.create("test.scope")
            every { getData() } returns gaugeData
            every { isEmpty() } returns false
        }
    }

    private fun createHistogramMetric(name: String): MetricData {
        val point =
            mockk<HistogramPointData> {
                every { getStartEpochNanos() } returns 1000000000L
                every { getEpochNanos() } returns 2000000000L
                every { getAttributes() } returns Attributes.empty()
                every { getCount() } returns 10L
                every { getSum() } returns 100.0
                every { getBoundaries() } returns listOf(1.0, 10.0, 100.0)
                every { getCounts() } returns listOf(2L, 3L, 4L, 1L)
                every { getExemplars() } returns emptyList()
            }

        val histogramData =
            mockk<HistogramData> {
                every { getPoints() } returns listOf(point)
                every { getAggregationTemporality() } returns AggregationTemporality.DELTA
            }

        return mockk<MetricData> {
            every { getName() } returns name
            every { getDescription() } returns "Test histogram description"
            every { getUnit() } returns "ms"
            every { getType() } returns MetricDataType.HISTOGRAM
            every { getResource() } returns Resource.getDefault()
            every { getInstrumentationScopeInfo() } returns InstrumentationScopeInfo.create("test.scope")
            every { getData() } returns histogramData
            every { isEmpty() } returns false
        }
    }

    private fun createExponentialHistogramMetric(name: String): MetricData {
        val point =
            mockk<ExponentialHistogramPointData> {
                every { getStartEpochNanos() } returns 1000000000L
                every { getEpochNanos() } returns 2000000000L
                every { getAttributes() } returns Attributes.empty()
                every { getCount() } returns 12L
                every { getSum() } returns 120.0
                every { getScale() } returns 2
                every { getZeroCount() } returns 5L
                every { getPositiveBuckets() } returns mockk<ExponentialHistogramBuckets>(relaxed = true)
                every { getNegativeBuckets() } returns mockk<ExponentialHistogramBuckets>(relaxed = true)
                every { getExemplars() } returns emptyList()
            }

        val expHistogramData =
            mockk<ExponentialHistogramData> {
                every { getPoints() } returns listOf(point)
                every { getAggregationTemporality() } returns AggregationTemporality.CUMULATIVE
            }

        return mockk<MetricData> {
            every { getName() } returns name
            every { getDescription() } returns "Test exponential histogram description"
            every { getUnit() } returns "bytes"
            every { getType() } returns MetricDataType.EXPONENTIAL_HISTOGRAM
            every { getResource() } returns Resource.getDefault()
            every { getInstrumentationScopeInfo() } returns InstrumentationScopeInfo.create("test.scope")
            every { getData() } returns expHistogramData
            every { isEmpty() } returns false
        }
    }

    private fun createSummaryMetric(name: String): MetricData {
        val point =
            mockk<SummaryPointData> {
                every { getStartEpochNanos() } returns 1000000000L
                every { getEpochNanos() } returns 2000000000L
                every { getAttributes() } returns Attributes.empty()
                every { getCount() } returns 20L
                every { getSum() } returns 200.0
                every { getValues() } returns listOf(mockk<ValueAtQuantile>(relaxed = true))
                every { getExemplars() } returns emptyList()
            }

        val summaryData =
            mockk<SummaryData> {
                every { getPoints() } returns listOf(point)
            }

        return mockk<MetricData> {
            every { getName() } returns name
            every { getDescription() } returns "Test summary description"
            every { getUnit() } returns "requests"
            every { getType() } returns MetricDataType.SUMMARY
            every { getResource() } returns Resource.getDefault()
            every { getInstrumentationScopeInfo() } returns InstrumentationScopeInfo.create("test.scope")
            every { getData() } returns summaryData
            every { isEmpty() } returns false
        }
    }
}
