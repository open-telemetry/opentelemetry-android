package network

import com.google.gson.annotations.SerializedName

data class UserToken(@SerializedName("token") val token: String)
data class UserStatus(@SerializedName("status") val status: String)
data class LogOutStatus(@SerializedName("logged_out") val loggedOut: Boolean)

