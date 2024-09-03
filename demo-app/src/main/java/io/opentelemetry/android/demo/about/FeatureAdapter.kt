package io.opentelemetry.android.demo.about

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.opentelemetry.android.demo.R

class FeatureAdapter(private val features: List<Feature>) : RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder>() {

    inner class FeatureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        private val arrowImageView: ImageView = itemView.findViewById(R.id.arrowImageView)

        fun bind(feature: Feature) {
            titleTextView.text = feature.title
            descriptionTextView.text = feature.description
            descriptionTextView.visibility = if (feature.isExpanded) View.VISIBLE else View.GONE
            arrowImageView.rotation = if (feature.isExpanded) 180f else 0f

            itemView.setOnClickListener {
                feature.isExpanded = !feature.isExpanded
                notifyItemChanged(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feature, parent, false)
        return FeatureViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        holder.bind(features[position])
    }

    override fun getItemCount(): Int = features.size

}

