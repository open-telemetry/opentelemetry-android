/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.opentelemetry.android.demo.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    // Renamed from _binding (default) due to ktlint problem below
    private var binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    // Currently failing ktlint due to
    // https://github.com/pinterest/ktlint/issues/2448
    // which hasn't hit the aging gradle spotless plugin yet
//    private val binding get() = _binding!!
    private fun getBinding(): FragmentHomeBinding {
        return binding!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = getBinding().root

        val textView: TextView = getBinding().textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
