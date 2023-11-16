# Android Instrumentation for URLConnection, HttpURLConnection and HttpsURLConnection

# Status : Experimental

Provides OpenTelemetry instrumentation for:
- [URLConnection](https://developer.android.com/reference/java/net/URLConnection),
- [HttpURLConnection](https://developer.android.com/reference/java/net/HttpURLConnection)
- [HttpsURLConnection](https://developer.android.com/reference/javax/net/ssl/HttpsURLConnection)

## Quickstart

### Overview

This plugin instruments calls to all relevant APIs (APIs that cause a connection to be established) in client's android application code. It wraps calls to these APIs to ensure the following:
- Context is added for distributed tracing before actual API is called (i.e before connection is established)
- Any exceptions thrown are recorded and spans are ended.
- If the getInputStream()/getErrorStream() APIs are used, the span is ended right after calling the API (as these are usually the last APIs to be called).

If getInputStream()/getErrorStream() APIs are not called, spans are ended by a periodically running thread that looks for any idle connections (read from previously but idle for >10s).

### Add these dependencies to your project

Replace `AUTO_HTTP_URL_INSTRUMENTATION_VERSION` with the [latest release](https://central.sonatype.com/search?q=g%3Aio.opentelemetry.android++a%3Ahttpurlconnection-library&smo=true).

Replace `BYTEBUDDY_VERSION` with the [latest release](https://search.maven.org/search?q=g:net.bytebuddy%20AND%20a:byte-buddy).

#### Byte buddy compilation plugin

This plugin leverages Android's [Transform API](https://developer.android.com/reference/tools/gradle-api/current/com/android/build/api/variant/ScopedArtifactsOperation#toTransform(com.android.build.api.artifact.ScopedArtifact,kotlin.Function1,kotlin.Function1,kotlin.Function1)) to instrument bytecode at compile time. You can find more info on its [repo page](https://github.com/raphw/byte-buddy/tree/master/byte-buddy-gradle-plugin/android-plugin.

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
You can configure the automatic instrumentation by using the setters in [HttpUrlInstrumentationConfig](library/src/main/java/io/opentelemetry/instrumentation/library/httpurlconnection/HttpUrlInstrumentationConfig.java)).

#### Required Configuration
It is required to **manually** schedule the following runnable to periodically run at the below given fixed interval to end any open spans if connection is left idle >10s.
- API to call to get the runnable: HttpUrlInstrumentationConfig.getReportIdleConnectionRunnable()
- API to call to get the fixed interval in milli seconds: HttpUrlInstrumentationConfig.getReportIdleConnectionInterval()

**For example**
Add the below code in the function where your application starts ( that could be onCreate() method of first Activity/Fragment/Service):
```Java
Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(HttpUrlInstrumentationConfig.getReportIdleConnectionRunnable(), 0, HttpUrlInstrumentationConfig.getReportIdleConnectionInterval(), TimeUnit.MILLISECONDS);
```

All other configurations are optional.

After adding the plugin and the dependencies to your project, and after doing the required configuration, your requests will be traced automatically.
