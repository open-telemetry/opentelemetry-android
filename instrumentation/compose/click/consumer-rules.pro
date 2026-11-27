# Keep the Compose internals class name. We need this for compose click capture.
-keep class androidx.compose.foundation.ClickableElement {
    <fields>;
}
-keep class androidx.compose.foundation.CombinedClickableElement {
    <fields>;
}
-keepnames class androidx.compose.foundation.selection.ToggleableElement
-keep class androidx.compose.ui.platform.TestTagElement {
    <fields>;
}