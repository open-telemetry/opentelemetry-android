package repo

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import app.AppContext


class TokenRepo(private val context: AppContext) {


    fun saveToken(token: String) {
        preferences().edit().putString(KEY_TOKEN, token).apply()
    }

    fun eraseToken() {
        preferences().edit().remove(KEY_TOKEN).apply()
    }

    fun isLoggedIn(): Boolean {
        return rawToken() != null
    }

    fun token(): String {
        return rawToken() ?: ""
    }

    private fun rawToken(): String? {
        return preferences().getString(KEY_TOKEN, null)
    }

    private fun preferences(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context.context)
    }

    companion object {
        private const val KEY_TOKEN = "key_token"
    }
}
