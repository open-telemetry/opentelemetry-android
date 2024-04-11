@file:Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")

package ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import app.DemoApp
import com.example.hello_otel.R
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import network.LogOutStatus
import network.UserStatus
import repo.TokenRepo

class LoggedInFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_logged_in, container, false)
    }

    override fun onViewCreated(loggedInView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(loggedInView, savedInstanceState)
        loggedInView.findViewById<View>(R.id.btn_check_in).setOnClickListener {
            checkIn(loggedInView.findViewById(R.id.tv_status))
        }

        loggedInView.findViewById<View>(R.id.btn_check_out).setOnClickListener {
            checkOut(loggedInView.findViewById(R.id.tv_status))
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setHasOptionsMenu(true)
    }

    private fun checkIn(tvStatus: TextView) {
        Single.defer { checkingIn() }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(AndroidLifecycleScopeProvider.from(this))
                .subscribe(
                        Consumer {
                            tvStatus.text = it.status
                        }
                )
    }

    private fun checkOut(tvStatus: TextView) {
        CheckOutRepo(requireContext()).checkingOut()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(AndroidLifecycleScopeProvider.from(this))
                .subscribe(
                        Consumer {
                            tvStatus.text = it.status
                        }
                )
    }

    private fun checkingIn(): Single<UserStatus> {
        return DemoApp.appScope(requireContext()).restApi().checkIn(TokenRepo(requireContext()).token())
    }


    private fun loggingOut(): Single<LogOutStatus> {
        return DemoApp.appScope(requireContext()).restApi().logOut()
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_logged_in, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> logOut()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logOut(): Boolean {
        loggingOutInternal()
        return true
    }

    private fun loggingOutInternal() {
        Single.defer { loggingOut() }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(AndroidLifecycleScopeProvider.from(this))
                .subscribe(this::onLogOutStatusReady)

    }

    private fun onLogOutStatusReady(status: LogOutStatus) {
        if (!status.loggedOut) {
            Toast.makeText(requireContext(), "Forcing logging out", Toast.LENGTH_SHORT).show()
        }
        TokenRepo(requireContext()).eraseToken()
        (requireActivity() as LoggedOutListener).onLoggedOut()
    }

    interface LoggedOutListener {
        fun onLoggedOut()
    }
}