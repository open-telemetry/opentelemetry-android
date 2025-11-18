# Kotlin-First Policy

The OpenTelemetry Android Agent was initially built when Java was the predominant language for Android development. Over the years, the platform has shifted towards being one that is primarily Kotlin, so this project will follow suit. This means that we consider the primary use case of this project to be apps written in Kotlin by folks who are familiar Android development in Kotlin.

In practice, we want our API and code to be structured with the Kotlin language and idioms in mind. This will allow the project to feel more native and be more readable to modern Android developers, thereby reduce one layer of friction of adopting OpenTelemetry: the Java-ness of it all.

Both the public API of the Agent and internal code will be progressively converted into Kotlin. It is a goal for us for all production code to one day be written exclusively in Kotlin.

## OpenTelemetry API Compatibility

OpenTelemetry Android Agent is currently built on the OTel Java SDK and ecosystem. It also exports the OTel Java API for recording OTel signals. This will be the case until official Kotlin versions are released and stabilized under the OpenTelemetry org, which is currently under development.

## Java Compatibility

Despite this policy, the Agent API should still be usable from Java, but they will not be optimized for it. For instance, a method that uses default arguments in Kotlin will require Java users to pass them in explicitly if no reasonable overloads can be provided.

As the API evolves, we will endeavor to preserve Java compile and runtime compatibility as much as possible. But we will only consider a change "breaking" if it affects the Kotlin use-case. As such, we advise that all calls to the Agent API are made from Kotlin if possible to prevent syntactic changes from breaking your app's build.
