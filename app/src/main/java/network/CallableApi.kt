package network

import retrofit2.http.GET
import retrofit2.http.Header

interface CallableApi {

    @GET("log_in")
    fun logIn(@Header("x-bypass") flag: Int): retrofit2.Call<UserToken>

    @GET("log_out")
    fun logOut(): retrofit2.Call<LogOutStatus>

    @GET("check_in")
    fun checkIn(@Header("token") flag: String): retrofit2.Call<UserStatus>

    @GET("check_out")
    fun checkout(): retrofit2.Call<UserStatus>


}