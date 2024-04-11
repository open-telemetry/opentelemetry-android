package com.example.hello_otel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.gson.JsonElement
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        return inflater.inflate(R.layout.fragment_first, container, false);


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvInfo = view.findViewById<TextView>(R.id.tv_text)
        view.findViewById<View>(R.id.button_first).setOnClickListener {
            showData(tvInfo)
        }
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

}