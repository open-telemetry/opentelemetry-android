package network

import io.opentelemetry.context.Context
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Tag

interface SingleApi {

    @GET("log_in")
    fun logInWithContext(@Tag context: Context, @Header("x-bypass") flag: Int): Single<UserToken>

    @GET("log_out")
    fun logOut(): Single<LogOutStatus>

    @POST("check_in")
    fun checkIn(@Tag context: Context? = null, @Body model: LocationModel, @Header("x-token") flag: String): Single<CheckInResult>

    @GET("check_out")
    fun checkoutWithoutBaggage(): Single<CheckInResult>

}