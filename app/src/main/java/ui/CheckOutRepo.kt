package ui

import app.DemoApp
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Context
import io.reactivex.Single
import network.UserStatus


class CheckOutRepo(private val context: android.content.Context) {

    fun checkingOut(withBaggage: Boolean): Single<UserStatus> {
        Context.current().with(ignoredBaggage()).makeCurrent().use {
            return if (withBaggage) callable() else rxjava()
        }
    }

    private fun callable(): Single<UserStatus> {
        return Single.fromCallable { checkingOutWithBaggage() }
    }

    private fun checkingOutWithBaggage(): UserStatus {
        Context.current().with(attachedBaggage()).makeCurrent().use {
            return DemoApp.appScope(context).restApi().checkout().execute().body()!!
        }
    }

    private fun rxjava(): Single<UserStatus> {
        return DemoApp.appScope(context).restApi().checkoutWithoutBaggage()
    }

    private fun ignoredBaggage(): Baggage {
        return Baggage.builder()
                .put("user.name", "jack")
                .put("user.id", "321")
                .build()
    }

    private fun attachedBaggage(): Baggage {
        return Baggage.builder()
                .put("user.name", "tony")
                .build()
    }

}

