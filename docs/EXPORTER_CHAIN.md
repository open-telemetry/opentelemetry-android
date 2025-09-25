# Exporter Delegate Chain (Spans / Logs / Metrics)

Status: draft

This document explains the default exporter delegation chain used by the OpenTelemetry Android RUM SDK and how you can customize or extend it with your own exporter layers.

## Goals

1. Avoid ANRs and StrictMode violations during SDK initialization by deferring expensive exporter setup off the main thread.
2. Ensure early telemetry (signals produced before exporters finish initializing) is not lost.
3. Provide optional durable (disk) buffering for offline / startup scenarios.
4. Allow applications to insert custom exporters (filtering, redaction, fan‑out, encryption, etc.).

## High-Level Flow

When you call `OpenTelemetryRum.builder()...build()`:

1. Lightweight in‑memory buffer exporters are installed immediately so spans, logs, and metrics can be accepted.
2. Actual exporter initialization (logging / OTLP + optional disk storage wiring) runs asynchronously on a background executor.
3. Once ready, buffered signals are flushed to the real exporters and all future exports are delegated directly.
4. If disk buffering is enabled, signals flow through disk first, then are later read back and exported to the network exporter.

## Default Chain (No Custom Exporters, Disk Buffering Enabled)

For each signal type (Span / LogRecord / Metric):

```text
BufferDelegating*Exporter  -->  *ToDiskExporter  -->  (Original default exporter)
    (in-memory buffer)           (writes batch          (e.g. LoggingSpanExporter,
                                 files to storage)       SystemOutLogRecordExporter,
                                                         LoggingMetricExporter)
```text

Later, a periodic scheduler reads batches from disk and replays them on the original exporter via:

```text
SignalFromDiskExporter -> *FromDiskExporter -> Original exporter
```

(Where `*` stands for `Span`, `LogRecord`, or `Metric`.)

## Default Chain (Disk Buffering Disabled)

```text
BufferDelegating*Exporter  -->  (Original default exporter)
```

No disk layer is inserted and no scheduled reader is enabled.

## Components

### BufferDelegating[Span|Log|Metric]Exporter

Internal in‑memory temporary exporters created immediately. They hold up to 5,000 items (per signal type) produced before the real delegate is attached. If the buffer fills, additional items are dropped with a warning log (`The <type> buffer was filled before export delegate set...`). After `setDelegate(...)` is invoked, the buffered data is exported, optional pending `flush()` / `shutdown()` calls are honored, and the buffer is cleared. From that point on the exporter no longer buffers anything: every new signal is delegated straight through to the next exporter in the chain (*ToDiskExporter if disk buffering is enabled, otherwise the customized/base exporter).

Source examples:

* `core/src/main/java/io/opentelemetry/android/export/BufferDelegatingSpanExporter.kt`
* `core/src/main/java/io/opentelemetry/android/export/BufferDelegatingLogExporter.kt`
* `core/src/main/java/io/opentelemetry/android/export/BufferDelegatingMetricExporter.kt`

### *ToDiskExporter (Optional Layer)

Wrappers provided by `io.opentelemetry.contrib.disk.buffering` that persist batches to disk before forwarding to the underlying ("original") exporter. Inserted only when `DiskBufferingConfig.enabled == true`.

### Original Exporters

The base exporters produced by the builder if you don't customize them:

* Spans: `LoggingSpanExporter`
* Logs: `SystemOutLogRecordExporter`
* Metrics: `LoggingMetricExporter`

In real deployments you usually replace these with OTLP exporters (e.g. OTLP/HTTP) by supplying customizers or by configuring upstream dependencies providing them.

### SignalFromDiskExporter & *FromDiskExporter

A coordinator (`SignalFromDiskExporter`) plus per-signal readers that pull batches from disk (one stored batch per original write call) and export them to the original exporter. A scheduler periodically invokes these to drain the on-device queue.

## Asynchronous Initialization

Exporter initialization (including disk capacity checks and creating `Storage` directories) is executed on `AsyncTask.THREAD_POOL_EXECUTOR` to prevent main-thread stalls. This was introduced after ANRs were observed when performing synchronous disk space checks (see PR #709). The memory buffering ensures telemetry created during this window is retained (up to buffer limits).

## Customizing the Chain

The builder exposes customizer hooks that let you wrap or replace the default exporter before the disk layer is added:

```java
OpenTelemetryRum.builder(application)
  .addSpanExporterCustomizer(exp -> new MyFilteringSpanExporter(exp))
  .addLogRecordExporterCustomizer(exp -> SpanAttributeRedactingLogExporter.wrap(exp))
  .addMetricExporterCustomizer(exp -> myMetricsFanOut(exp))
  .build();
```

Customizer semantics (build time vs. runtime order):

* Start with the SDK's default exporter (e.g. `LoggingSpanExporter`) – call this the base exporter.
* Each customizer is invoked in the exact order it was registered. It receives the exporter built so far and returns a (possibly wrapped) exporter. This produces a nested chain. If you register A then B then C, the resulting nesting is: `C(B(A(base)))`.
* If disk buffering is enabled, the fully customized exporter (the outermost custom wrapper in code, i.e. `C(...)` in the example) is then wrapped by `*ToDiskExporter` so data is persisted to disk before reaching any of the custom wrappers.
* Finally a `BufferDelegating*Exporter` wraps the whole thing to capture early telemetry while async initialization finishes. After `setDelegate(...)` it becomes a pass‑through.
* Runtime data flow therefore in the A,B,C example (registered in that order) is:

```text
BufferDelegating*Exporter → *ToDiskExporter (if enabled) → C → B → A → base exporter
```

Notes:

* Registration order (A,B,C) is outside‑in at build time, but runtime export order is the reverse (C,B,A) because of nested wrapping.
* The disk buffering layer is not added via a customizer; it is applied after all customizers are evaluated.
* If disk buffering is disabled the flow simply omits that layer:

```text
BufferDelegating*Exporter → C → B → A → base exporter
```

Summary (concise): Once the exporter is built and all customizers have been applied, that result is wrapped by a disk buffering exporter (if enabled) and then finally wrapped by a `BufferDelegating*Exporter`.

You control the final network/export sink by returning it from the last customizer. For example, to send spans via OTLP HTTP:

```java
builder.addSpanExporterCustomizer(prev -> OtlpHttpSpanExporter.builder()
    .setEndpoint("https://collector.example.com/v1/traces")
    .build());
```

(You can wrap the OTLP exporter again if you need additional behavior.)

## Flush and Shutdown Behavior

If `flush()` or `shutdown()` is invoked before delegates are attached, the `DelegatingExporter` stores a pending result. Once the real delegate is set:

1. Buffered data is exported
2. A flush is issued if it was pending
3. A shutdown is issued if it was pending
4. Pending futures complete with the real delegate result

## Failure and Backpressure Characteristics

* Buffer Overflow: If more than 5,000 signals of a type are produced before delegate attachment, newer signals beyond capacity are dropped (a warning is logged). Consider reducing startup emission volume or initializing earlier if this occurs.
* Disk Layer: If disk initialization fails, the SDK logs an error and proceeds WITHOUT disk buffering (the chain reverts to memory buffer -> original exporter). The scheduled disk reader is disabled in this case.
* From-Disk Export Failures: Batches that fail to export remain on disk (subject to age / size pruning rules governed by `DiskBufferingConfig`).
* To-Disk Export Failures: If disk buffering is enabled and initialized but a batch cannot be written (e.g., I/O error or size constraint), that batch is immediately forwarded to the underlying exporter (skipping disk for that batch) to avoid data loss. Subsequent batches continue attempting disk writes.

## Configuration References

`DiskBufferingConfig` (see `core/src/main/java/io/opentelemetry/android/features/diskbuffering/DiskBufferingConfig.kt`) controls:

* Enable/disable disk buffering
* Max cache folder size
* Max file size
* File age thresholds for read/write rotation
* Optional directory override

## When To Add Your Own Layer

Common customization patterns:

* Filtering / Sampling: Drop or modify signals before disk/network (privacy, volume control)
* Redaction / PII Scrubbing: Remove sensitive attributes centrally
* Fan-out: Send data to multiple exporters (e.g., internal analytics + OTLP)
* Encryption: Encrypt payloads prior to writing to disk (wrap before `*ToDiskExporter`) or prior to network send (wrap after disk exporter if you want encrypted at rest)

Example filtering span exporter:

```java
class MyFilteringSpanExporter implements SpanExporter {
  private final SpanExporter delegate;
  MyFilteringSpanExporter(SpanExporter delegate) { this.delegate = delegate; }
  @Override public CompletableResultCode export(Collection<SpanData> spans) {
    List<SpanData> filtered = spans.stream()
      .filter(s -> !"debug".equals(s.getAttributes().get(stringKey("env"))))
      .toList();
    return delegate.export(filtered);
  }
  @Override public CompletableResultCode flush() { return delegate.flush(); }
  @Override public CompletableResultCode shutdown() { return delegate.shutdown(); }
}
```

Register in builder:

```java
builder.addSpanExporterCustomizer(exp -> new MyFilteringSpanExporter(exp));
```
