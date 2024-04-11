package mini

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Header

interface RestApi {
    @GET("log_in")
    fun login(@Header("x-bypass") flag: Int): Single<UserToken>

    @GET("auth")
    fun loginRetrofit2Call(@Header("x-bypass") flag: Int): retrofit2.Call<UserToken>

}