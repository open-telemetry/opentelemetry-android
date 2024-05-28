/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.opentelemetry.android.demo.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {
    // Renamed from _binding (default) due to ktlint problem below
    private var binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    // Currently failing ktlint due to
    // https://github.com/pinterest/ktlint/issues/2448
    // which hasn't hit the aging gradle spotless plugin yet
//    private val binding get() = _binding!!
    fun getBinding(): FragmentDashboardBinding {
        return binding!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = getBinding().root

        val textView: TextView = getBinding().textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
