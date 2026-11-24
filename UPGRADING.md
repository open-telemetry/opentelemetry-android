# Upgrading to OpenTelemetry Android 2.0

This document describes the changes between OpenTelemetry Android 1.0 and 2.0 that may affect existing integrations, along with instructions on how to migrate your code and configuration.

## Table of Contents

- [Toolchain and Build Requirements](#toolchain-and-build-requirements)
- [Semantic Conventions](#semantic-conventions)
- [SDK Initialization Events](#sdk-initialization-events)
- [Session Management](#session-management)
- [Instrumentation Renames and Discovery](#instrumentation-renames-and-discovery)
- [Removed and Reworked APIs](#removed-and-reworked-apis)

---

## Toolchain and Build Requirements

### Android API Level (minSdk)
- **Change:** The minimum supported Android API level increased from 21 (Lollipop) to 23 (Marshmallow).
- **Migration:** Ensure your application's `minSdk` is set to 23 or higher in `build.gradle.kts`. Note that API levels 23–25 still require core library desugaring.

### Kotlin and Compiler Versions
- **Change:** The minimum supported Kotlin version increased to 2.1 (and 2.3 when using `coroutines-library`).
- **Migration:** Ensure the Kotlin Gradle plugin is upgraded to 2.1 or newer.

### Gradle, AGP, and Compile SDK
- **Change:** The minimum supported build environment requires AGP 8.12.0+, Gradle 8.13+, JDK 17+, and `compileSdk` 36 or higher. Published artifacts declare a `minCompileSdk` of 36, and `android-agent` / `okhttp3-agent` declare 37 (due to OkHttp dependencies).
- **Migration:** Upgrade your build configuration to use AGP 8.12.0+ and `compileSdk = 36` (or `37` if using `android-agent`).

---

## Semantic Conventions

### Experimental Semantic Conventions Enabled by Default
- **Change:** Starting in version 1.5.0 and 1.7.0, OpenTelemetry Android defaults to the latest experimental OpenTelemetry semantic conventions:
  - Screen attribute: `screen.name` is replaced by `app.screen.name`.
  - Previous screen attribute: `last.screen.name` is replaced by `app.screen.previous.name`.
  - Crash event: `device.crash` is replaced by `app.crash`.
  - Network status attribute: `network.status` is replaced by `network.connection.type`.
- **Migration:** Update your backend queries, dashboards, and alerting rules to use the new semantic convention attribute names. If you need temporary backward compatibility while migrating your backend, you can retain the legacy attribute names in the DSL:
  ```kotlin
  OpenTelemetryRumInitializer.initialize(application) {
      semanticConventions {
          useLatestExperimental = false
      }
  }
  ```

---

## SDK Initialization Events

### Disabled by Default
- **Change:** Starting in version 1.8.0, SDK initialization events (`app.initialization`) are disabled by default to reduce default telemetry volume.
- **Migration:** If you rely on SDK initialization events, explicitly enable them in the configuration DSL:
  ```kotlin
  OpenTelemetryRumInitializer.initialize(application) {
      enableSdkInitializationEvents()
  }
  ```

---

## Session Management

### Background Inactivity
- **Change:** Inversion of background session keep-alive behavior: looking up a session ID no longer resets the background inactivity timer. Background telemetry (such as WorkManager jobs or background syncs) no longer prevents a background session from expiring.
- **Migration:** No configuration change is required for most applications. If background tasks require distinct session tracking, observe session changes using `addSessionObserver` or rely on new sessions starting upon user return to foreground.

---

## Instrumentation Renames and Discovery

### Java Crash Instrumentation Renamed
- **Change:** The Java crash instrumentation was renamed from `crash` to `java-crash` to distinguish it from `native-crash`.
- **Migration:** If you programmatically disable or configure the crash instrumentation by name, update the identifier to `"java-crash"`:
  ```kotlin
  OpenTelemetryRumInitializer.initialize(application) {
      instrumentation {
          disabledInstrumentations.add("java-crash")
      }
  }
  ```

### Instrumentation Discovery Control
- **Change:** Automatic instrumentation discovery can now be disabled via the Android Agent DSL.
- **Migration:** Use `disableInstrumentationAutoDiscovery()` if you prefer to explicitly register only selected instrumentations.

---

## Removed and Reworked APIs

### Legacy SDK Ready Listeners Removed
- **Change:** Deprecated `RumReadyListener` and `sdkReadyListener` were removed.
- **Migration:** Use `addOtelReadyListener` instead:
  ```kotlin
  openTelemetryRumBuilder.addOtelReadyListener { rum ->
      // Access initialized OpenTelemetryRum instance
  }
  ```

### Disk Buffering Export Scheduling
- **Change:** `setExportScheduleHandler` was removed in favor of `setExportScheduleEnablement`.
- **Migration:** Use `setExportScheduleEnablement(ScheduleEnablement.ENABLED)` or configure disk buffering through the agent DSL:
  ```kotlin
  OpenTelemetryRumInitializer.initialize(application) {
      diskBuffering {
          enabled(true)
          exportPeriod = 10.seconds
      }
  }
  ```

### Slow Rendering Instrumentation
- **Change:** Slow rendering instrumentation no longer produces zero-duration spans. Instead, frame performance is reported via `app.jank` log events carrying frame render duration metrics.
- **Migration:** Query `app.jank` log events rather than spans for slow and frozen frame analysis.

### OkHttp Instrumentation Package Reorganization
- **Change:** OkHttp instrumentation classes were moved to dedicated package namespaces.
- **Migration:** Update imports from legacy packages to `io.opentelemetry.instrumentation.library.okhttp.v3_0` (or `agent` counterpart).

