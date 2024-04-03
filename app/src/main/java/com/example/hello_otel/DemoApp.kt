package com.example.hello_otel

import android.app.Application
import androidx.fragment.app.Fragment
import timber.log.Timber

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DemoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        plantTimberLogger()
    }

    private fun plantTimberLogger() {
        Timber.plant(Timber.DebugTree())
        Timber.i("Demo App started")
    }
}