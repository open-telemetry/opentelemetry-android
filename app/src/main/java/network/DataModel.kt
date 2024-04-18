package network

import com.google.gson.annotations.SerializedName

data class UserToken(@SerializedName("token") val token: String)
data class CheckInResult(@SerializedName("status") val status: String)
data class LogOutStatus(@SerializedName("logged_out") val loggedOut: Boolean)
data class LocationEntity(@SerializedName("lat") val lat: Double, @SerializedName("lng") val lng: Double)
data class LocationModel(@SerializedName("list") val list: List<LocationEntity>)

