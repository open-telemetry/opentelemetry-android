# Telemetry module schema

Instrumentation modules that participate in generated telemetry documentation contain a
`telemetry.yaml` file. The `mergeAllTelemetryDocs` task owns this generated file; contributors
should not edit it manually.

Version 1 has this structure:

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

`type` is one of `event`, `log`, or `span`. Metrics are deferred from schema version 1.
`registry_id` is `null` when the signal has no resolved semantic convention group. Attribute
`registry` is `local`, `upstream`, or `none`. Captured values are intentionally excluded.
