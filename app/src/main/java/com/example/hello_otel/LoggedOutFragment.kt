@file:Suppress("DEPRECATION")

package com.example.hello_otel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.gson.JsonElement
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class LoggedOutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_logged_out, container, false);
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setHasOptionsMenu(true)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.btn_login_success).setOnClickListener {
            logInSuccess()
        }
        view.findViewById<View>(R.id.btn_login_failure).setOnClickListener {
            logInFailure()
        }
    }

    private fun logInFailure() {
        showLogInFailure()
    }

    private fun showLogInFailure() {
        Toast.makeText(requireContext(), "Username/Password wrong", Toast.LENGTH_SHORT).show()
    }

    private fun logInSuccess() {
        AuthRepo(requireContext()).saveToken("1234")
        (requireActivity() as LoggedInListener).onLoggedIn()
    }

    private fun showData(tvInfo: TextView) {
        Single.defer { uberTraceId() }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(AndroidLifecycleScopeProvider.from(this))
                .subscribe(
                        Consumer {
                            tvInfo.text = it.toString()
                        }
                )
    }

    private fun uberTraceId(): Single<JsonElement> {
        return DemoApp.appScope(requireContext()).restApi().profile("1234");
    }

    interface LoggedInListener{
        fun onLoggedIn()
    }

}