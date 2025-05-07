
# Android Log Instrumentation

Status: development

The Android Log instrumentation transforms calls to `android.uti.Log.x` to emit
OTEL log record.

## Telemetry

This instrumentation produces the following telemetry:

### Log

* Type: Log
* Description: A log message
* Attributes:
    * `android.log.tag`: The tag passed to `android.uti.Log.x`
    * `exception.stacktrace` - Optional. See the ([semconv here](https://github.com/open-telemetry/semantic-conventions/blob/727700406f9e6cc3f4e4680a81c4c28f2eb71569/docs/attributes-registry/exception.md#exception-stacktrace)).
    * `exception.type` - Optional. See the ([semconv here](https://github.com/open-telemetry/semantic-conventions/blob/727700406f9e6cc3f4e4680a81c4c28f2eb71569/docs/attributes-registry/exception.md#exception-type))
