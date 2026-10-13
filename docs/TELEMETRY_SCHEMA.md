# Telemetry module schema

Instrumentation modules that participate in generated telemetry documentation contain a
`telemetry.yaml` file. The `mergeAllTelemetryDocs` task owns this generated file; contributors
should not edit it manually.

An event-only instrumentation has this structure:

```yaml
schema_version: 1
module: "view-click"
scopes:
  - "io.opentelemetry.android.instrumentation.view.click"
signals:
  - type: event
    name: "app.screen.click"
    scope: "io.opentelemetry.android.instrumentation.view.click"
    registry_id: "event.app.screen.click"
    attributes:
      - name: "app.screen.coordinate.x"
        type: int
        registry: upstream
```

A span-only instrumentation has a separate file:

```yaml
schema_version: 1
module: "okhttp3"
scopes:
  - "io.opentelemetry.okhttp-3.0"
signals:
  - type: span
    scope: "io.opentelemetry.okhttp-3.0"
    registry_id: "span.http.client"
    attributes:
      - name: "http.request.method"
        type: string
        registry: upstream
```

`type` is one of `event`, `log`, or `span`. Metrics are deferred from schema version 1.
Events use their unique event name. Spans omit their runtime name and use `registry_id` as their
stable identity. If span inference finds no unique registry group, `registry_id` is `unidentified`
and the candidate ambiguity is logged. For non-span signals, `registry_id` is `null` when no
resolved group exists. Attribute `registry` is `local`, `upstream`, or `none`. Captured values are
intentionally excluded.
