@file:Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")

package com.example.hello_otel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

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
            checkIn()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setHasOptionsMenu(true)
    }

    private fun checkIn() {
        Toast.makeText(requireContext(), "Checked in", Toast.LENGTH_SHORT).show()
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
        eraseState()
        (requireActivity() as LoggedOutListener).onLoggedOut()
        return true
    }

    private fun eraseState() {
        AuthRepo(requireContext()).eraseToken()
    }

    interface LoggedOutListener {
        fun onLoggedOut()
    }
}