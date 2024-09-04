package io.opentelemetry.android.demo.about

data class Feature(
    val title: String,
    val description: String,
    var isExpanded: Boolean = false
)

fun getFeatureList(): List<Feature> {
    return listOf(
        Feature(
            "Android Activity Lifecycle Monitoring",
            """
            - Automatically captures spans for key lifecycle events:
                - Created: Includes `onCreate`, `onStart`, `onResume`,
                - Paused: Includes `onPause`,
                - Stopped: Includes `onStop`,
                - Destroyed: Includes `onDestroy`.
            - This covers the entire Activity lifecycle, providing detailed insights into each phase.
            """.trimIndent()
        ),
        Feature(
            "ANR Detection",
            """
            - Automatically detects and reports ANRs in the app.
            - ANR events are captured as spans with detailed stack traces, providing insights into the exact operations that caused the ANR.
            - The span includes key attributes such as `screen.name`, `session.id`, and network information to assist in diagnosing the issue.
            """.trimIndent()
        ),
        Feature(
            "Slow Render Detection",
            """
            - Automatically detects instances of slow rendering within the app.
            - Slow render events are captured as spans, providing information on when and where rendering delays occurred.
            - The span includes attributes such as `activity.name`, `screen.name`, `count`, and network details to help diagnose performance issues.
            """.trimIndent()
        ),
        Feature(
            "Manual Instrumentation",
            """
            - Provides access to the OpenTelemetry APIs for manual instrumentation, allowing developers to create custom spans and events as needed.
            """.trimIndent()
        )
    )
}