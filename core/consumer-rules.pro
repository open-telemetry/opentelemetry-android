-dontwarn com.google.auto.value.AutoValue$Builder
-dontwarn com.google.auto.value.AutoValue$CopyAnnotations
-dontwarn com.google.auto.value.AutoValue
-dontwarn com.google.auto.value.extension.memoized.Memoized
-dontwarn com.google.errorprone.annotations.**
-dontwarn io.grpc.Channel
-dontwarn io.grpc.MethodDescriptor$Builder
-dontwarn io.grpc.MethodDescriptor$Marshaller
-dontwarn io.grpc.MethodDescriptor$MethodType
-dontwarn io.grpc.MethodDescriptor
-dontwarn io.grpc.stub.AbstractFutureStub
-dontwarn io.grpc.stub.AbstractStub$StubFactory
-dontwarn io.grpc.stub.AbstractStub
-dontwarn io.opentelemetry.sdk.autoconfigure.**

# EntityUtil in opentelemetry-sdk-common invokes these package-private members via reflection.
-keepclassmembers class io.opentelemetry.sdk.resources.Resource,io.opentelemetry.sdk.resources.AutoValue_Resource {
    java.util.Collection getEntities();
    io.opentelemetry.api.common.Attributes getUnassociatedAttributes();
}
-keepclassmembers class io.opentelemetry.sdk.resources.ResourceBuilder {
    io.opentelemetry.sdk.resources.ResourceBuilder addEntity(io.opentelemetry.sdk.resources.internal.Entity);
}
