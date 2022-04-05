---

<p align="center">
  <strong>
    <a href="#getting-started">Getting Started</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="CONTRIBUTING.md">Getting Involved</a>
  </strong>
</p>

<p align="center">
   <a href="https://github.com/signalfx/splunk-otel-android/actions?query=workflow%3A%22Continuous+Build%22">
     <img alt="Build Status" src="https://img.shields.io/github/workflow/status/signalfx/splunk-otel-android/Continuous%20Build?style=for-the-badge">
   </a>
<!--   <a href="https://github.com/signalfx/splunk-otel-android/releases"> -->
<!--     <img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/signalfx/splunk-otel-android?include_prereleases&style=for-the-badge"> -->
<!--   </a> -->
  <img alt="Beta" src="https://img.shields.io/badge/status-beta-informational?style=for-the-badge">
</p>

<p align="center">
  <strong>
    <a href="docs/faq.md">FAQ</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="SECURITY.md">Security</a>
  </strong>
</p>

---

# Splunk OpenTelemetry Instrumentation for Android

Splunk RUM Product Documentation can be found [here](https://docs.splunk.com/Observability/rum/intro-to-rum.html#nav-Introduction-to-Splunk-RUM).

> :construction: This project is currently in **BETA**. It is officially supported by Splunk. However, breaking changes MAY be introduced.

## Features

* Crash reporting
* ANR detection
* Network change detection
* Full Android Activity and Fragment lifecycle monitoring
* Access to the OpenTelemetry APIs for manual instrumentation
* SplunkRum APIs for creating custom RUM events and reporting exceptions
* Access to an OkHttp3 Call.Factory implementation for monitoring http client requests
* APIs to redact any span from export, or change span attributes before export
* Slow / frozen render detection
* Offline buffering of telemetry via storage

## Getting Started

### Prerequisites

This library supports Android API levels 21 and above, with [core library desugaring][desugar]
enabled.

WARNING: It is *VERY IMPORTANT* that you are have enabled [core library desugaring][desugar] in the build for
your app. If you have not done this, and you are targetting API levels below 26, your app will crash
on devices running API level < 26.

### Getting the library

There are two options for bringing in this library as a dependency for your Android app:

#### Use as a gradle dependency from maven central:

Add Maven Central as a maven repository to the `repositories` section of your main build.gradle:

```
allprojects {
    repositories {
        google()
...
        mavenCentral()
    }
}
```

Then, add the latest release as a dependency in your application's build.gradle file.

```
dependencies {
...
    implementation ("com.splunk:splunk-otel-android:0.12.0")
...
}
```

#### Build the library locally:

First, clone this repository locally:

```
git clone https://github.com/signalfx/splunk-otel-android.git
```

Then build locally and publish to your local maven repository:

```
./gradlew publishToMavenLocal
```

Make sure you have `mavenLocal()` as a repository in your application's main `build.gradle`:

```
allprojects {
    repositories {
        google()
...
        mavenLocal()
    }
}
```

Then, add the locally built library as a dependency in your application's build.gradle:

```
dependencies {
...
    implementation ("com.splunk:splunk-otel-android:0.14.0-SNAPSHOT")
...
}
```

### Configuration

In order to configure the Splunk RUM library, you will need to know three things:

* Your Splunk realm.
    * The realm can be found in your Splunk Observability UI in the Account Settings page.
* Your RUM access token.
    * You can find or create a RUM access token in the Splunk Observability UI, in your Organization
      Settings.
    * Important: this access token *must* have the `RUM` authorization scope to work.
* The name of your application.

Here is an example of a the very minimal configuration which uses these 3 values:

```java
class MyApplication extends Application {
    private final String realm = "<realm>";
    private final String rumAccessToken = "<your_RUM_access_token>";
    private final Config config = SplunkRum.newConfigBuilder()
            .realm(realm)
            .rumAccessToken(rumAccessToken)
            .applicationName("My Android App")
            .build();
}
```

There are other options available on the `Config.Builder` instance, including enabling debug mode
and enabling/disabling various instrumentation features.

### Initialization

To initialize the Splunk RUM monitoring library, from your `android.app.Application` instance,
simply call the static initializer in your `Application.onCreate()` implementation:

```java
class MyApplication extends Application {
    //...

    @Override
    public void onCreate() {
        super.onCreate();
        SplunkRum.initialize(config, this);
    }
}
```

Examples of this process can be seen in the sample application included in this repository in
the `sample-app` submodule.

### Instrument WebViews using the Browser RUM agent

Mobile RUM instrumentation and Browser RUM instrumentation can be used
simultaneously by sharing the `splunk.rumSessionId` between both
instrumentations to see RUM data combined in one stream.

The following Android snippet shows how to integrate Android RUM with
Splunk Browser RUM:

``` java
import android.webkit.WebView;
import com.splunk.rum.SplunkRum;

//...
/* 
Make sure that the WebView instance only loads pages under 
your control and instrumented with Splunk Browser RUM. The 
integrateWithBrowserRum() method can expose the splunk.rumSessionId
of your user to every site/page loaded in the WebView instance.
*/
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
   super.onViewCreated(view, savedInstanceState);
   binding.webView.setWebViewClient(new LocalContentWebViewClient(assetLoader));
   binding.webView.loadUrl("https://subdomain.example.com/instrumented-page.html");

   binding.webView.getSettings().setJavaScriptEnabled(true);
   binding.webView.addJavascriptInterface(new WebAppInterface(getContext()), "Android");
   SplunkRum.getInstance().integrateWithBrowserRum(binding.webView);
}
```

You can now retrieve the session id in your JS code using `SplunkRumNative.getNativeSessionId()`. For example:

```html
<p>Session ID: <span id="session_id"></span></p>

<script type="text/javascript">
    document.getElementById("session_id").innerHTML = SplunkRumNative.getNativeSessionId();
</script>
```
### Advanced Usage

#### Additional `Config.Builder` options.

There are a number of optional configuration options that can be specified via the `Config.Builder`
when initializing your instance of the SplunkRum API:

(Note: full javadoc can be found at [javadoc.io][javadoc-url])

- `deploymentEnvironment(String)` :
  This option will set the Splunk environment attribute on the spans that are generated by the
  instrumentation.
- `beaconEndpoint(String)` :
  Rather than using the `realm(String)` configuration option, you can use this method to explicitly
  give the full URL of the RUM ingest endpoint.
- `debugEnabled(boolean)` :
  Enabling `debug` mode will turn on the opentelemetry logging span exporter, which can be useful
  when debugging instrumentation issues. Additional logging may also be turned on with this option.
- `crashReportingEnabled(boolean)` :
  This option can be used to turn off the crash reporting feature.
- `networkMonitorEnabled(boolean)` :
  This option can be used to turn off the network monitoring feature.
- `anrDetectionEnabled(boolean)` :
  This option can be used to turn off the ANR detection feature.
- `globalAttributes(Attributes)` :
  This option allows you to add a set of OpenTelemetry Attributes to be appended to every span
  generated by the library.
- `filterSpans(Consumer<SpanFilterBuilder>)` :
  This can be used to provide customizations of the spans that are emitted by the library. Examples
  include: removing spans altogether from export, removing span attributes, changing span attributes
  or changing the span name. See the javadoc on the `SpanFilterBuilder` class for more details.
- `slowRenderingDetectionPollInterval(Duration)` :
  Set/change the default polling interval for slow/frozen render detection.
  Default is 1000ms. Value must be positive. 
- `slowRenderingDetectionEnabled(boolean)` :
  Disables the detection of slow frame renders. Enabled by default.
- `diskBufferingEnabled(boolean)` : 
  Enables the storage-based buffering of telemetry. 
  This setting is useful when instrumenting applications that might work offline for extended periods of time.

#### APIs provided by the `SplunkRum` instance:

(Note: full javadoc can be found at [javadoc.io][javadoc-url])

- The SplunkRum instrumentation uses OpenTelemetry APIs and semantic conventions for span
  generation. If you have need of writing your own manual instrumentation, the SplunkRum instance
  gives you direct access to the instance of OpenTelemetry that is being used via
  the `getOpenTelemetry()` method. For details on writing manual instrumentation, please refer to
  the [OpenTelemetry docs](https://opentelemetry.io/docs/java/manual_instrumentation/)
  and [examples](https://github.com/open-telemetry/opentelemetry-java/tree/main/examples).
- The SplunkRum instance exposes the RUM session ID, in case you wish to provide this to your users
  for troubleshooting purposes. This session ID is generated randomly and contains no PII
  whatsoever.
- If you wish to record some simple Events or Workflows, the SplunkRum instances provides APIs for
  that:
    - `addRumEvent(String, Attributes)` : record a simple "zero duration" span with the provided
      name and attributes.
    - `startWorkflow(String) : Span` : This method allows you to start a Splunk RUM "workflow" for
      which metrics will be recorded by the RUM backend. The returned OpenTelemetry `Span`
      instance *must* be ended for this workflow to be recorded.
- To record a custom Error or Exception, SplunkRum exposes an `addRumException(Throwable)` method,
  and one that also accepts a set of `Attributes`. These exceptions will appear as errors in the RUM
  UI, and error metrics will be recorded for them.
- If you need to update the set of "global attributes" that were initially configured, you can do
  that via one of two methods on the SplunkRum instance:  `setGlobalAttribute(AttributeKey)`
  or `updateGlobalAttributes(Consumer<AttributesBuilder> attributesUpdater)`. The former will add or
  update a single attribute, and the latter allows bulk updating of the attributes.
- To add OpenTelemetry instrumentation to your OkHttp3 client, SplunkRum provides an
  okhttp `Call.Factory` wrapper that can be applied to your client. See
  the `createRumOkHttpCallFactory(OkHttpClient)` for details.

#### Detection of slow or frozen renders

By default, Splunk RUM detects and reports slow or frozen screen renders.
To disable this feature, call `.slowRenderingDetectionEnabled(false)` on the 
`Config.Builder`.

Splunk RUM defines renders as slow or frozen following the [Android Vitals definitions](https://developer.android.com/topic/performance/vitals/frozen):

| Category | Speed   | spanName      | Attribute   |
|----------|---------|---------------|-------------|
| Slow     | >16ms  | slowRenders    | count       | 
| Frozen   | >700ms | frozenRenders  | count       |

## Customizing screen names

By default, the instrumentation uses the simple class name of each `Fragment`
and `Activity` type as the value of the `screen.name` attribute. To customize the
screen name, use the `@RumScreenName` annotation.

For example, the following activity appears with the `screen.name` 
attribute set to the value "Buttercup":

```java
@RumScreenName("Buttercup")
public class MainActivity extends Activity {
    ...
}
```

## Troubleshooting

- If you see runtime errors related to Java 8 interfaces and classes, make sure you have
  enabled `coreLibraryDesugaring` per the official Android [documentation][desugar].
- Please report any bugs either here as a Github issue, or with official Splunk support channels.

## Sample Application

This repository includes a sample application that can show off a few features of our mobile RUM
product.

In order to build and run the sample application, you will need to configure a `local.properties`
file in the root of the project. It will need to have two properties configured:

```properties
rum.realm=<realm>
rum.access.token=<a valid Splunk RUM access token for the realm>
```

# License and versioning

The Splunk Android RUM Instrumentation is released under the terms of the Apache Software License
version 2.0. See
[the license file](./LICENSE) for more details.

>ℹ️&nbsp;&nbsp;SignalFx was acquired by Splunk in October 2019. See [Splunk SignalFx](https://www.splunk.com/en_us/investor-relations/acquisitions/signalfx.html) for more information.

[desugar]: https://developer.android.com/studio/write/java8-support#library-desugaring

[javadoc-url]: https://www.javadoc.io/doc/com.splunk/splunk-otel-android/latest/com/splunk/rum/SplunkRum.html
