# StrictMode Policy and OpenTelemetry Android

This document explains the current policy, known sources, and mitigation options for Android StrictMode
violations when using the OpenTelemetry Android SDK ("android-agent") and underlying OpenTelemetry Java SDK.

StrictMode is a development tool that surfaces potentially expensive operations (e.g. disk/network I/O) on
threads where they could cause UI jank or ANRs. The OpenTelemetry Android SDK aims to minimize undesirable
impact to startup and frame rendering. However:

- Some one‑time initialization I/O is currently unavoidable.
- Completely eliminating all StrictMode disk read signals would add significant complexity or overhead.

We group known violations into three categories:

1. Avoidable – we intend to fix or have open issues / PRs.  
2. Optional / Avoidable by app choice – can be prevented by disabling an optional feature OR by applying an early, low‑cost mitigation (e.g. setting a property very early).  
3. Acceptable / Unavoidable – one‑time, low‑latency initialization that cannot practically be prevented without disproportionate complexity, performance cost, or brittle ordering requirements.  

> IMPORTANT: StrictMode reports *any* disk read/write on the main thread, regardless of cost. A reported
> violation does not automatically mean a user‑visible freeze. Always measure real device startup / frame
> metrics before drawing conclusions.

## Summary Table

| Source / Stack (Representative) | Category | Description | Mitigation / Workaround |
|---------------------------------|----------|-------------|--------------------------|
| `io.opentelemetry.context.LazyStorage.<clinit>` SPI resource scan (ServiceLoader) | 2 | One‑time classpath resource + service index scan across DEX/JAR elements to initialize context storage in OpenTelemetry Java | Pre‑seed the default context storage via system property to avoid ServiceLoader disk scan during first span; see below. |
| Offline persistence initialization (creating / listing storage dir) | 2 | Disk reads/writes to enable offline batching of telemetry (creates cache subdir, may stat existing files) | Disable offline buffering (`DiskBufferingConfig.enabled = false` when configuring, or omit enabling the feature) if startup StrictMode cleanliness is critical – see `core/src/main/java/io/opentelemetry/android/features/diskbuffering/DiskBufferingConfig.kt` |
| Crash / ANR signal handlers registration (proc / system reads) | 3 | Light, one‑time metadata collection (may read `/proc/self/` entries, system traces) | No direct workaround; expected to be minimal on typical hardware—measure on low‑end devices |
| Reading network state / registering listeners | 2 | Accessing system services (e.g. `ConnectivityManager`, possibly reading system settings) | Disable specific instrumentation modules (network, slow/frozen frame) or remove their Gradle dependency |

We will expand this table as additional sources are identified. Contributions welcome.

## Known Primary Violation: Context Storage Lazy Initialization

Stack traces similar to:

```text
StrictMode policy violation; ~duration=168 ms: android.os.strictmode.DiskReadViolation
    at java.util.ServiceLoader$LazyClassPathLookupIterator.hasNext(ServiceLoader.java:...)
    at io.opentelemetry.context.LazyStorage.createStorage(LazyStorage.java:107)
    at io.opentelemetry.context.LazyStorage.<clinit>(LazyStorage.java:80)
    at io.opentelemetry.context.ContextStorage.get(ContextStorage.java:72)
    at io.opentelemetry.context.Context.current(Context.java:92)
    at io.opentelemetry.sdk.trace.SdkSpanBuilder.startSpan(...)
```

Cause: The OpenTelemetry Java SDK lazily resolves a `ContextStorage` implementation using `ServiceLoader`. The
first time a span is started (or context accessed) on the main thread, a disk read can occur while the runtime
scans Class-Path / DEX resource indices (service descriptors) to locate an implementation.

References:

- OTel Java issue: <https://github.com/open-telemetry/opentelemetry-java/issues/7600>
- `LazyStorage` source (may move across versions): <https://github.com/open-telemetry/opentelemetry-java/blob/main/context/src/main/java/io/opentelemetry/context/LazyStorage.java>

### Workaround: Pre‑seed the Default Context Storage

If you do not provide a custom context storage provider, you can set a system property **as early as possible** (before
any OpenTelemetry API, agent, or auto-initialization code runs) to short‑circuit the `ServiceLoader` scan.

Important details:

- Set it before any OpenTelemetry API usage.
- ContentProvider-based auto-initialization may run earlier; if that happens and can't be disabled, accept the one-time scan.
- Applies per process.
- Do not set it if you use a custom ContextStorage implementation.

Kotlin (e.g. in a custom `Application` `attachBaseContext` or very early in `onCreate`):

```kotlin
override fun attachBaseContext(base: Context?) {
  // Prevent lazy ServiceLoader disk scan on first span creation
  System.setProperty("io.opentelemetry.context.contextStorageProvider", "default")
  super.attachBaseContext(base)
}
```

Java equivalent:

```java
public class MyApplication extends Application {
  @Override public void attachBaseContext(Context base) {
    System.setProperty("io.opentelemetry.context.contextStorageProvider", "default");
    super.attachBaseContext(base);
  }
}
```

Notes:

- Must run before any OpenTelemetry API call (including automatic instrumentation) to be effective.
- Does nothing if you actually supply a custom provider (omit it in that case).
- Eliminates the specific `LazyStorage` triggered disk read StrictMode warning in most scenarios.

### Alternative: Initialize Off Main Thread

If your app architecture supports delaying the first span until after a background initialization phase has run,
you can trigger a dummy context access / span start in a background thread:

```kotlin
Executors.newSingleThreadExecutor().execute {
  val tracer = GlobalOpenTelemetry.getTracer("warmup")
  tracer.spanBuilder("otel.warmup").startSpan().end()
}
```

This forces the one‑time initialization off the UI thread. Be sure any feature relying on early spans (e.g.
startup telemetry) still meets your requirements.

## Feature-Specific Violations and How To Bypass

| Feature | Potential Main Thread I/O (Examples) | Bypass / Disable (Current State) | Trade‑offs |
|---------|--------------------------------------|----------------------------------|------------|
| Offline buffering / storage | Create cache dir, list existing batch files, stat file sizes | Disable via `DiskBufferingConfig(enabled = false)` (or omit enabling); if no runtime toggle exposed, remove related dependency | No offline durability; data loss when offline |
| Crash reporting | Read `/proc/self/stat`, signal handler setup, collect app/package metadata | Remove crash instrumentation dependency (runtime toggle TBD) | No automatic crash spans / attributes |
| ANR detection | Install handlers / read traces or binder timeouts (light reads) | Remove ANR instrumentation dependency | Lose ANR diagnostics |
| Slow / frozen frame detection | Access frame metrics / choreographer callbacks (minimal disk I/O) | Remove slowrendering instrumentation dependency | Lose frame performance telemetry |
| Network change detection | Query `ConnectivityManager`, register network callbacks (system service I/O) | Remove network instrumentation dependency | Lose network state enrichment |
| Context storage init | ServiceLoader resource scan | Pre‑seed property OR off-main-thread warmup | None (once-only cost) |

(Exact configuration flags / Gradle coordinates to disable each module should be documented here as those features become generally available. PRs welcome.)

## Policy

1. One‑time, low‑latency (< ~5ms guideline) main‑thread disk reads during initialization are acceptable when they occur exactly once per process (concurrent first access still counts as one logical init) or are realistically unavoidable due to platform/JVM constraints.
2. Where feasible we expose mitigations (early system property, background warmup, feature toggles) so apps can eliminate or shift this work.
3. StrictMode violations > ~16ms that are easily/consistently reproducible and attributable to OpenTelemetry main‑thread I/O may be filed as issues. Please include stack trace, timing, reproduction steps, and enabled modules so we can prioritize actual user impact.
4. The list of sources will evolve; new SDK versions, Android platform changes, or dependency updates can surface new one‑time reads. We’ll adjust documentation as patterns emerge.
5. Always validate real performance signals (startup time, frame timing, ANR rate) before optimizing purely for zero StrictMode entries; benign one‑time reads without user impact are lower priority than sustained latency.

## Reporting Additional Violations

If you find a StrictMode violation that appears frequently or adds noticeable startup time:

1. Capture the full StrictMode stack trace with timestamps and (if possible) duration.  
2. Note which OpenTelemetry modules are enabled and your SDK version.  
3. Open an issue (or comment on an existing one) in `opentelemetry-android` with the above information and steps to reproduce.  
4. Provide device class info (API level, CPU/RAM tier).  

## References

- OTel Android issue (policy): <https://github.com/open-telemetry/opentelemetry-android/issues/1188>
- OTel Java issue (LazyStorage SPI scan): <https://github.com/open-telemetry/opentelemetry-java/issues/7600>
- OTel Java `LazyStorage` source: <https://github.com/open-telemetry/opentelemetry-java/blob/main/context/src/main/java/io/opentelemetry/context/LazyStorage.java>
- Disk buffering configuration (`DiskBufferingConfig`): `core/src/main/java/io/opentelemetry/android/features/diskbuffering/DiskBufferingConfig.kt`
- Exporter delegate chain overview: `docs/EXPORTER_CHAIN.md`
