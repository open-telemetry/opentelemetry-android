package com.example.hello_otel

import android.content.Context
import androidx.preference.PreferenceManager


class AuthRepo(private val context: Context) {


    fun saveToken(token: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun eraseToken() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(KEY_TOKEN).apply()
    }

    fun isLoggedIn(): Boolean {
        val string = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_TOKEN, null)
        return string != null
    }

    companion object {
        private const val KEY_TOKEN = "key_token"
    }
}
