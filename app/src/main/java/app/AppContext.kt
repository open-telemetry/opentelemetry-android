package app

import android.content.Context

data class AppContext(val context: Context) {

    companion object {

        fun from(context: Context): AppContext {
            return AppContext(context)
        }
    }
}