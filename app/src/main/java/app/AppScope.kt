package app

import network.RestApi
import okhttp3.mockwebserver.RecordedRequest

interface AppScope {

    fun dumpData(): List<Any>
    fun restApi(): RestApi
    fun recordedRequest(): RecordedRequest?
}