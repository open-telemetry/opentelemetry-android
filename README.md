---

<p align="center">
  <strong>
    <a href="#getting-started">Getting Started</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="CONTRIBUTING.md">Getting Involved</a>
  </strong>
</p>

<p align="center">
<!--   <a href="https://github.com/signalfx/splunk-otel-android/actions?query=workflow%3A%22CI+build%22"> -->
<!--     <img alt="Build Status" src="https://img.shields.io/github/workflow/status/signalfx/splunk-otel-android/CI%20build?style=for-the-badge"> -->
<!--   </a> -->
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

# Splunk Android RUM Instrumentation

> :construction: This project is currently in **BETA**.


## Getting Started 

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
    implementation ("com.splunk:splunk-otel-android:0.2.0")
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
    implementation ("com.splunk:splunk-otel-android:0.3.0-SNAPSHOT")
...
}
```

### Features
* Crash reporting
* ANR detection
* Network change detection
* Full Android Activity and Fragment lifecycle monitoring
* Access to the OpenTelemetry APIs for manual instrumentation
* SplunkRum APIs for creating custom RUM events and reporting exceptions
* Access to an OkHttp3 Interceptor implementation for monitoring http client requests

### Configuration

In order to configure the Splunk RUM library, you will need to know three things:
* Your Splunk realm. 
  * The realm can be found in your Splunk Observability UI in the Account Settings page.
* Your RUM access token.  
  * You can find or create a RUM access token in the Splunk Observability UI, in your Organization Settings.
  * Important: this access token *must* have the `RUM` authorization scope to work. 
* The name of your application.

Here is an example of a the very minimal configuration which uses these 3 values:
```java
        String beaconUrl = "<realm>";
        String rumAccessToken = "<your_RUM_access_token>";
        Config config = SplunkRum.newConfigBuilder()
                .realm(realm)
                .rumAccessToken(rumAuth)
                .applicationName("My Android App")
                .build();
```

There are other options available on the `Config.Builder` instance, including enabling debug mode
and enabling/disabling various instrumentation features.

### Initialization

To initialize the Splunk RUM monitoring library, from your `android.app.Application` instance, 
simply call the static initializer in your `Application.onCreate()` implementation:
```java
        SplunkRum.initialize(config, this);
```

Examples of this process can be seen in the sample application included in this repository in the `sample-app` submodule.

## Sample Application
This repository includes a sample application that can show off a few features of our mobile RUM product. 

In order to build and run the sample application, you will need to configure a `local.properties` file
in the root of the project. It will need to have two properties configured:

```properties
rum.access.token=<a valid Splunk RUM access token>
rum.beacon.url=https://rum-ingest.<realm>.signalfx.com/v1/rum
```

# License and versioning

The Splunk Android RUM Instrumentation is released under the terms of the Apache Software License version 2.0. See
[the license file](./LICENSE) for more details.
