package com.example.hello_otel

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider.*
import com.uber.autodispose.autoDispose
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val fab: FloatingActionButton by lazy { findViewById(R.id.fab) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val findViewById: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(findViewById)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
        val rootSpan= DemoApp.appScope(application).rootSpan()
        rootSpan?.let {
            it.setAttribute("activity","activity_created")
            it.end()
            val traceId = rootSpan.spanContext.traceId
            val spanId = rootSpan.spanContext.spanId
            Timber.tag(DemoApp.LOG_TAG).i("Ended trace_id:$traceId,span_id:$spanId")
        }
        fab.setOnClickListener { view ->
            login(view)
        }
    }

    private fun login(view: View) {
        showLoginError(view)
    }

    private fun showLoginError(view: View) {
        auth(0)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(from(this))
                .subscribe(
                        { showLoginSuccess(it) },
                        { loginError(it, view) }
                )
    }

    private fun triggerLoginSuccess() {
        auth(1).observeOn(AndroidSchedulers.mainThread())
                .autoDispose(from(this))
                .subscribe(
                        Consumer {
                            showLoginSuccess(it)
                        },
                )

    }

    private fun showLoginSuccess(it: UserToken) {
        Toast.makeText(this, "fetched user token:$it", Toast.LENGTH_SHORT).show()
    }

    private fun auth(flag: Int): Single<UserToken> {
        return Single.defer{
            DemoApp.appScope(application).restApi()
                    .login(flag)
        }
                .subscribeOn(Schedulers.io())
    }


    private fun loginError(it: Throwable, view: View) {
        Snackbar.make(view, "login error: ${it.message}", Snackbar.LENGTH_LONG)
                .setAction("try again") {
                    triggerLoginSuccess()
                }
                .setAnchorView(R.id.fab).show()
    }

    private fun showOtelLog() {
        Single.fromCallable { uberTraceId() }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(from(this))
                .subscribe(
                Consumer {
                    showData(it)
                }
                )


    }

    private fun showData(data: String) {

        Snackbar.make(fab, data, Snackbar.LENGTH_LONG)
                .setAction("Got it") {

                }
                .setAnchorView(R.id.fab).show()
    }

    private fun uberTraceId(): String {
        val rumSessionId = DemoApp.appScope(application).openTelemetryRum().rumSessionId
        Timber.i("Detected rumSessionId:$rumSessionId")
        val uberTraceId = DemoApp.appScope(application).recordedRequest()?.headers?.get("uber-trace-id")
        return "$uberTraceId"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> showDialog()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDialog(): Boolean {
        showOtelLog()
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}