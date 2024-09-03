package io.opentelemetry.android.demo.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.opentelemetry.android.demo.R

class AppFeaturesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var featureAdapter: FeatureAdapter

    private val features = getFeatureList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_app_features, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)

        featureAdapter = FeatureAdapter(features)
        recyclerView.adapter = featureAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        return view
    }
}
