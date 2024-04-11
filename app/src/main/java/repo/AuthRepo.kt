package repo

import app.AppContext
import app.DemoApp
import network.UserToken
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class AuthRepo(private val app: AppContext) {

    private fun authInternal(flag: Int): Single<UserToken> {
        return Single.defer {
            DemoApp.appScope(app).singleApi().logIn(flag)
        }.subscribeOn(Schedulers.io())
    }


    fun auth(success: Boolean): Single<UserToken> {
        return authInternal(if (success) 1 else 0)
    }


}