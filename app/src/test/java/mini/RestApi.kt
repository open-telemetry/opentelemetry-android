package mini

import retrofit2.http.GET
import retrofit2.http.Header

interface RestApi {
    @GET("auth")
    fun login(@Header("x-bypass") flag: Int): retrofit2.Call<UserToken>

    @GET("profile")
    fun profile(@Header("token") flag: String): retrofit2.Call<UserStatus>
}