# Fix Summary: Disk Buffering Export Frequency Optimization

## Issue Description
The disk buffering exporter was attempting to export signals every 10 seconds, resulting in excessive IO operations and battery drain. During an 8-hour workday, this equated to approximately **2,880 export attempts** per device, which is unsustainable for enterprise applications where devices are used throughout the day.

## Root Cause
The original implementation used hardcoded 10-second delays in two places:
1. **PeriodicWork**: `SECONDS_FOR_NEXT_LOOP = 10L`
2. **DefaultExportScheduler**: `DELAY_BEFORE_NEXT_EXPORT_IN_MILLIS = TimeUnit.SECONDS.toMillis(10)`

This aggressive polling frequency provided minimal benefit for real-time signal export while significantly impacting device battery life and backend load.

## Solution Implemented

### 1. **Increased Default Export Frequency to 1 Minute**

#### PeriodicWorkImpl.kt
- Changed `DEFAULT_LOOP_INTERVAL_MILLIS` from 10 seconds to **60,000 milliseconds (1 minute)**
- This reduces export attempts from 2,880 to **480 per 8-hour workday** (~83% reduction)

```kotlin
companion object {
    internal const val DEFAULT_LOOP_INTERVAL_MILLIS: Long = 60000L // 1 minute
}
```

#### DefaultExportScheduler.kt
- Changed default `exportScheduleDelayMillis` parameter to **1 minute**
- Maintains consistency with the periodic work loop interval

```kotlin
class DefaultExportScheduler(
    periodicWorkProvider: () -> PeriodicWork,
    private val exportScheduleDelayMillis: Long = TimeUnit.MINUTES.toMillis(1),
) : PeriodicRunnable(periodicWorkProvider)
```

### 2. **Added Configurable Export Frequency Parameter**

#### DiskBufferingConfig.kt
- Introduced `exportScheduleDelayMillis: Long` parameter with default value of 1 minute
- Added comprehensive documentation explaining the trade-off between real-time-ness and battery consumption
- Parameter is propagated through the builder pattern via `create()` factory method

```kotlin
/**
 * The delay in milliseconds between consecutive export attempts. Defaults to 1 minute (60000 ms).
 * A higher value reduces battery consumption, while a lower value provides more real-time exporting.
 */
val exportScheduleDelayMillis: Long = DEFAULT_EXPORT_SCHEDULE_DELAY_MILLIS,

companion object {
    const val DEFAULT_EXPORT_SCHEDULE_DELAY_MILLIS: Long = 60000L // 1 minute
}
```

### 3. **Integration with OpenTelemetryRumBuilder**

#### OpenTelemetryRumBuilder.kt
- Properly passes `diskBufferingConfig.exportScheduleDelayMillis` to the `DefaultExportScheduler` constructor
- Ensures the configured value is respected throughout the application lifecycle

```kotlin
DefaultExportScheduler(
    services::periodicWork, 
    diskBufferingConfig.exportScheduleDelayMillis
)
```

### 4. **Comprehensive Test Coverage**

#### PeriodicWorkTest.kt
- Tests verify the 60-second delay between periodic work executions
- Three test scenarios:
  1. **Execute enqueued work on start**: Validates that multiple tasks run in a single worker thread after the delay
  2. **Check for pending work after a delay**: Ensures tasks are properly queued and executed at correct intervals
  3. **Remove delegated work from further executions**: Confirms one-time execution of completed tasks

#### DefaultExportSchedulerTest.kt
- **Verify minimum delay**: Confirms default 1-minute delay is applied
- **Verify custom delay can be set**: Tests that custom delays can be configured
- **Export behavior tests**: Ensures proper handling of signal exports and error conditions

All tests pass successfully, validating the correctness of the implementation.

## Impact Analysis

### Battery Life Improvement
| Metric | Before | After | Reduction |
|--------|--------|-------|-----------|
| Exports per 8 hours | 2,880 | 480 | 83.3% |
| Exports per hour | 360 | 60 | 83.3% |
| IO Frequency | Every 10s | Every 60s | 6x |

### User Experience
- **Default Behavior**: 1-minute export delay provides a good balance between real-time data and battery efficiency
- **Customization**: Enterprise apps can adjust `exportScheduleDelayMillis` to:
  - Reduce to 30 seconds for critical real-time monitoring
  - Increase to 5 minutes for battery-constrained environments
  - Set to any custom value matching their requirements

### Backend Load Reduction
With typical enterprise deployments having hundreds or thousands of devices:
- **Before**: 2,880 exports/device/8 hours × 1,000 devices = 2.88M exports
- **After**: 480 exports/device/8 hours × 1,000 devices = 480K exports
- **Reduction**: ~83% decrease in backend load

## Files Modified

| File | Change |
|------|--------|
| `services/src/main/java/.../PeriodicWorkImpl.kt` | DEFAULT_LOOP_INTERVAL_MILLIS: 10s → 60s |
| `core/src/main/java/.../DiskBufferingConfig.kt` | Added `exportScheduleDelayMillis` parameter |
| `core/src/main/java/.../DefaultExportScheduler.kt` | Default delay: 10s → 60s (configurable) |
| `core/src/main/java/.../OpenTelemetryRumBuilder.kt` | Pass configured delay to scheduler |
| `services/src/test/java/.../PeriodicWorkTest.kt` | Validates 60s delay behavior |
| `core/src/test/java/.../DefaultExportSchedulerTest.kt` | Validates configurable delays |

## Configuration Usage

### Default Configuration
```kotlin
val config = DiskBufferingConfig(enabled = true)
// exportScheduleDelayMillis defaults to 60,000 ms (1 minute)
```

### Custom Configuration
```kotlin
// Set custom export frequency (e.g., 30 seconds)
val config = DiskBufferingConfig.create(
    enabled = true,
    exportScheduleDelayMillis = TimeUnit.SECONDS.toMillis(30)
)

// Set for battery optimization (e.g., 5 minutes)
val config = DiskBufferingConfig.create(
    enabled = true,
    exportScheduleDelayMillis = TimeUnit.MINUTES.toMillis(5)
)
```

## Backward Compatibility
- ✅ All changes are backward compatible
- ✅ Existing code will automatically use the more efficient 1-minute default
- ✅ Applications that explicitly configure the delay will continue to work as expected
- ✅ No breaking changes to public APIs

## Testing Results
```
BUILD SUCCESSFUL in 48s
140 actionable tasks: 140 executed
All tests passed ✓
```
