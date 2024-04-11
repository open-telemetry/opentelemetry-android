package ui

import app.DemoApp
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.reactivex.Single
import network.UserStatus


class CheckOutRepo(private val context: android.content.Context) {

    fun checkingOut(): Single<UserStatus> {
        return Single.fromCallable { checkOutInternal() }
    }


    private fun checkOutInternal(): UserStatus {
        Context.current().with(rootBaggage()).makeCurrent().use {
            return DemoApp.appScope(context).restApi().checkout().execute().body()!!
        }
    }

    private fun rootBaggage(): Baggage {
        return Baggage.builder()
                .put("user.name", "jack")
                .put("user.id", "321")
                .build()
    }

}

