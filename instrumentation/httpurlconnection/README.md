# Android Instrumentation for URLConnection, HttpURLConnection and HttpsURLConnection

## Status : Experimental

Provides OpenTelemetry instrumentation for:
- [URLConnection](https://developer.android.com/reference/java/net/URLConnection)
- [HttpURLConnection](https://developer.android.com/reference/java/net/HttpURLConnection)
- [HttpsURLConnection](https://developer.android.com/reference/javax/net/ssl/HttpsURLConnection)

## Quickstart

### Overview

This plugin enhances the Android application host code by instrumenting all critical APIs (specifically those that initiate a connection). It intercepts calls to these APIs to ensure the following actions are performed:
- Context is added for distributed tracing before actual API is called (i.e before connection is initiated).
- Traces and spans are generated and properly closed.
- Any exceptions thrown are recorded.

A span associated with a given request is concluded in the following scenarios:
- When the getInputStream()/getErrorStream() APIs are called, the span concludes after the stream is fully read, an IOException is encountered, or the stream is closed.
- When the disconnect API is called.

Spans won't be automatically ended and reported otherwise. If any of your URLConnection requests do not call the span concluding APIs mentioned above, refer the section entitled ["Scheduling Harvester Thread"](#scheduling-harvester-thread). This section provides guidance on setting up a recurring thread that identifies unreported, idle connections and ends/reports any open spans associated with them. Idle connections are those that have been read from previously but have been inactive for a particular configurable time interval (defaults to 10 seconds).

> The minimum supported Android SDK version is 21, though it will also instrument APIs added in the Android SDK version 24 when running on devices with API level 24 and above.

> If your project's minSdk is lower than 26, then you must enable
> [corelib desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring).
>
> If your project's minSdk is lower than 24, in order to run the app built on debug, you need to add the following property in `gradle.properties` file:
> -  If AGP <= 8.3.0, set `android.enableDexingArtifactTransform=false`
> - if AGP > 8.3.0, set `android.useFullClasspathForDexingTransform=true`
>
> For the full context for these workaround, please see
> [this issue](https://issuetracker.google.com/issues/334281968) for AGP <= 8.3.0
> or [this issue](https://issuetracker.google.com/issues/230454566#comment18) for AGP > 8.3.0.

### Add these dependencies to your project

Replace `AUTO_HTTP_URL_INSTRUMENTATION_VERSION` with the [latest release](https://central.sonatype.com/search?q=g%3Aio.opentelemetry.android++a%3Ahttpurlconnection-library&smo=true).

Replace `BYTEBUDDY_VERSION` with the [latest release](https://search.maven.org/search?q=g:net.bytebuddy%20AND%20a:byte-buddy).

#### Byte buddy compilation plugin

This plugin leverages Android's [Transform API](https://developer.android.com/reference/tools/gradle-api/current/com/android/build/api/variant/ScopedArtifactsOperation#toTransform(com.android.build.api.artifact.ScopedArtifact,kotlin.Function1,kotlin.Function1,kotlin.Function1)) to instrument bytecode at compile time. You can find more info on its [repo page](https://github.com/raphw/byte-buddy/tree/master/byte-buddy-gradle-plugin/android-plugin).

```groovy
plugins {
    id 'net.bytebuddy.byte-buddy-gradle-plugin' version 'BYTEBUDDY_VERSION'
}
```

#### Project dependencies

```kotlin
implementation("io.opentelemetry.android:httpurlconnection-library:AUTO_HTTP_URL_INSTRUMENTATION_VERSION")
byteBuddy("io.opentelemetry.android:httpurlconnection-agent:AUTO_HTTP_URL_INSTRUMENTATION_VERSION")
```

### Configurations

#### Scheduling Harvester Thread

To schedule a periodically running thread to conclude/report spans on any unreported, idle connections, add the below code in the function where your application starts ( that could be onCreate() method of first Activity/Fragment/Service):
```Java
HttpUrlInstrumentation instrumentation = AndroidInstrumentationLoader.getInstrumentation(HttpUrlInstrumentation.class);
instrumentation.setConnectionInactivityTimeoutMs(customTimeoutValue); //This is optional. Replace customTimeoutValue with a long data type value which denotes the connection inactivity timeout in milli seconds. Defaults to 10000ms
Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(instrumentation.getReportIdleConnectionRunnable(), 0, instrumentation.getReportIdleConnectionInterval(), TimeUnit.MILLISECONDS);
```

- `instrumentation.getReportIdleConnectionRunnable()` is the API to get the runnable.
- By default, connections inactive for 10000ms are reported. To optionally change the connection inactivity timeout call `instrumentation.setConnectionInactivityTimeoutMs(customTimeoutValue)` API as shown in above code snippet.
- It is efficient to run the harvester thread at the same time interval as the connection inactivity timeout used to identify the connections to be reported. `instrumentation.getReportIdleConnectionInterval()` is the API to get the same connection inactivity timeout interval (milliseconds) you have configured or the default value of 10000ms if not configured.

#### Other Optional Configurations
You can optionally configure the automatic instrumentation by using the setters from [HttpUrlInstrumentation](library/src/main/java/io/opentelemetry/instrumentation/library/httpurlconnection/HttpUrlInstrumentation.java)
instance provided via the AndroidInstrumentationLoader as shown below:

```java
HttpUrlInstrumentation instrumentation = AndroidInstrumentationLoader.getInstrumentation(HttpUrlInstrumentation.class);

// Call `instrumentation` setters.
```

> [!NOTE]
> You must make sure to apply any configurations **before** initializing your OpenTelemetryRum
> instance (i.e. calling OpenTelemetryRum.builder()...build()). Otherwise your configs won't be
> taken into account during the RUM initialization process.

After adding the plugin and the dependencies to your project, and after doing the required configurations, your requests will be traced automatically.
