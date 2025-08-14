configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "com.squareup.okhttp3" && requested.name == "okhttp-jvm") {
                useTarget("com.squareup.okhttp3:okhttp-android:${requested.version}")
                because("choosing okhttp-android over okhttp-jvm")
            }
        }
    }
}

