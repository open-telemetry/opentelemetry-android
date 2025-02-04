
# Crash Instrumentation

Status: experimental

The crash instrumentation detects uncaught exceptions in the user
application and reports these occurrences as telemetry.

## Telemetry

This instrumentation produces the following telemetry:

### Crash

* Type: Event (*)
* Name: `device.crash` (see note below)
* Description: An event that is generated for exceptions not handled by user code.
* Attributes:
    * `event.name`: `device.crash` (see note below)
    * `exception.message` ([see semconv here](https://github.com/open-telemetry/semantic-conventions/blob/727700406f9e6cc3f4e4680a81c4c28f2eb71569/docs/attributes-registry/exception.md#exception-message))
    * `exception.stacktrace` ([see semconv here](https://github.com/open-telemetry/semantic-conventions/blob/727700406f9e6cc3f4e4680a81c4c28f2eb71569/docs/attributes-registry/exception.md#exception-stacktrace))
    * `exception.type` ([see semconv here](https://github.com/open-telemetry/semantic-conventions/blob/727700406f9e6cc3f4e4680a81c4c28f2eb71569/docs/attributes-registry/exception.md#exception-type))
    * `thread.id` ([see semconv here](https://github.com/open-telemetry/semantic-conventions/blob/727700406f9e6cc3f4e4680a81c4c28f2eb71569/docs/attributes-registry/thread.md#thread-id))
    * `thread.name` ([see semconv here](https://github.com/open-telemetry/semantic-conventions/blob/727700406f9e6cc3f4e4680a81c4c28f2eb71569/docs/attributes-registry/thread.md#thread-name))

(*) Note: This event is currently a malformed LogRecord. It will use correct event fields
    and semantics after the java sdk has these features available.

Note: This instrumentation supports additional user-configurable `AttributeExtractors` that
may set additional attributes from the given `CrashDetails` (`Thread` and `Throwable`).
