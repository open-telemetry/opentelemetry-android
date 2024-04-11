package network

import app.DemoApp
import com.chuckerteam.chucker.api.ChuckerInterceptor
import interceptor.FirstFixedInterceptor
import interceptor.SecondFixedInterceptor
import io.opentelemetry.instrumentation.library.okhttp.v3_0.internal.OkHttp3Singletons
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object MockWebServerUtil {


    fun initServer(mockWebServer: MockWebServer) {
        Completable.fromAction {
            mockWebServer.start()
            val mockDispatcher: Dispatcher = object : Dispatcher() {
                @Throws(InterruptedException::class)
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path
                    return when (path) {
                        "/rt/v1/log_in" -> mockResponse(request.getHeader("x-bypass") == "1")
                        "/rt/v1/check_in" -> MockResponse().setResponseCode(200).setBody("""
                            {
                              "status": "Checked In" 
                            }
                        """.trimIndent())

                        "/rt/v1/check_out" -> MockResponse().setResponseCode(200).setBody("""
                            {
                              "status": "Checked Out" 
                            }
                        """.trimIndent())

                        "/rt/v1/log_out" -> MockResponse().setResponseCode(200).setBody("""
                            {
                              "logged_out": true 
                            }
                        """.trimIndent())

                        else -> MockResponse().setResponseCode(404)
                    }
                }

                private fun mockResponse(alwaysSuccess: Boolean): MockResponse {
                    return if (alwaysSuccess) MockResponse().setResponseCode(200)
                            .setBody("""
                                {
                                    "token":"1234"
                                }
                            """.trimIndent())
                    else MockResponse().setResponseCode(401)
                            .setBody("""
                                {
                                    "error":{
                                        "code":1
                                        "message":"Incorrect password"
                                    }
                                }
                            """.trimIndent())
                }
            }
            mockWebServer.dispatcher = mockDispatcher
        }.subscribeOn(Schedulers.computation()).subscribe()

    }

}