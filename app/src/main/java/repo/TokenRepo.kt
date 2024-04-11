package repo

import android.content.Context
import androidx.preference.PreferenceManager


class TokenRepo(private val context: Context) {


    fun saveToken(token: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun eraseToken() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(KEY_TOKEN).apply()
    }

    fun isLoggedIn(): Boolean {
        val string = rawToken()
        return string != null
    }

    fun token(): String {
        val string = rawToken()
        return string?:""
    }

    private fun rawToken(): String? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_TOKEN, null)
    }

    companion object {
        private const val KEY_TOKEN = "key_token"
    }
}
