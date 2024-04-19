@file:Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")

package ui

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import app.AppContext
import app.DemoApp
import com.example.hello_otel.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.baggage.BaggageBuilder
import io.opentelemetry.context.Context
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import network.CheckInResult
import network.LocationEntity
import network.LocationModel
import network.LogOutStatus
import repo.CheckOutRepo
import repo.TokenStore

class LoggedInFragment : Fragment() {

    private lateinit var tvStatus: TextView
    private var progressDialogFragment: ProgressDialogFragment? = null
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }
    private val locationRequest by lazy {
        LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 3000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_logged_in, container, false)
    }

    override fun onViewCreated(loggedInView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(loggedInView, savedInstanceState)
        tvStatus = loggedInView.findViewById(R.id.tv_status)
        loggedInView.findViewById<View>(R.id.btn_check_in).setOnClickListener {
            kickOffCheckIn()
        }

        loggedInView.findViewById<View>(R.id.btn_check_out).setOnClickListener {
            checkingOut(checkout(true))
        }
        loggedInView.findViewById<View>(R.id.btn_check_out_without_baggage).setOnClickListener {
            checkingOut(checkout(false))
        }
    }

    /**
     * We want to show all 3 different baggages attached at the different in the http request header.
     */
    private fun kickOffCheckIn() {
        val builder = Baggage.builder()
        builder.put("check_in_started", System.currentTimeMillis().toString())
        if (ActivityCompat.checkSelfPermission(requireActivity(), ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireActivity(), ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
            return
        }

        showProcessDialog()
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                onLocationResultReady(locationResult, builder)
            }
        }, Looper.getMainLooper())
    }

    private fun newContext(): Context {
        return Context.current()
    }


    private fun showProcessDialog() {
        progressDialogFragment = ProgressDialogFragment()
        progressDialogFragment?.show(childFragmentManager, ProgressDialogFragment.TAG)
    }

    private fun LocationCallback.onLocationResultReady(locationResult: LocationResult, baggageBuilder: BaggageBuilder) {
        fusedLocationClient.removeLocationUpdates(this)
        baggageBuilder.put("location_fetched", System.currentTimeMillis().toString())
        checkInWithLocation(locationResult, baggageBuilder)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }

    private fun onPermissionDenied() {
        Toast.makeText(requireActivity(), "Permission Denied", Toast.LENGTH_SHORT).show()
    }

    private fun onPermissionGranted() {
        kickOffCheckIn()
    }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        setHasOptionsMenu(true)
    }

    private fun checkInWithLocation(location: LocationResult, baggageBuilder: BaggageBuilder) {
        Single.defer { checkingIn(locationResultModel(location), baggageBuilder) }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(AndroidLifecycleScopeProvider.from(this))
                .subscribe(this::updateStatus)
    }

    private fun locationResultModel(location: LocationResult): LocationModel {
        return LocationModel(location.locations.map { LocationEntity(it.latitude, it.longitude) })
    }

    private fun updateStatus(it: CheckInResult) {
        this.tvStatus.text = it.status
        progressDialogFragment?.dismiss()
    }

    private fun checkingOut(call: Single<CheckInResult>) {
        call
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(AndroidLifecycleScopeProvider.from(this))
                .subscribe(this::updateStatus)
    }

    private fun checkout(withBaggage: Boolean): Single<CheckInResult> {
        return CheckOutRepo(appContext()).checkingOut(withBaggage)
    }

    private fun checkingIn(locationModel: LocationModel, baggageBuilder: BaggageBuilder): Single<CheckInResult> {
        baggageBuilder.put("sending_network", System.currentTimeMillis().toString())
        val baggage = baggageBuilder.build()
        val newContext = Context.current().with(baggage)
        return DemoApp.appScope(appContext()).singleApi().checkIn(newContext, locationModel, TokenStore(appContext()).token())
    }


    private fun loggingOut(): Single<LogOutStatus> {
        return DemoApp.appScope(appContext()).singleApi().logOut()
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
        TokenStore(appContext()).eraseToken()
        (requireActivity() as LoggedOutListener).onLoggedOut()
    }

    private fun appContext() = AppContext.from(requireContext())

    private fun attachedCheckInStarted(): Baggage {
        val builder = Baggage.builder()
        return builder
                .put("check_in_started", System.currentTimeMillis().toString())
                .build()
    }


    private fun attachedLocationFetched(): Baggage {
        return Baggage.builder()
                .put("location_fetched", System.currentTimeMillis().toString())
                .build()
    }


    interface LoggedOutListener {
        fun onLoggedOut()
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 0

    }
}

