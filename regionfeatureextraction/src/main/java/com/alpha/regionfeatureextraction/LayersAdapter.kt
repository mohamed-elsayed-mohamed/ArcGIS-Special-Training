package com.alpha.regionfeatureextraction

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alpha.regionfeatureextraction.databinding.ItemLayerBinding
import com.esri.arcgisruntime.layers.Layer
import com.esri.arcgisruntime.mapping.LayerList

class LayersAdapter: RecyclerView.Adapter<LayersAdapter.LayersViewHolder>() {

    private var layers: LayerList? = null

    fun setData(layers: LayerList){
        this.layers = layers
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayersViewHolder = LayersViewHolder(
        ItemLayerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: LayersViewHolder, position: Int) = holder.bind(layers?.get(position))

    override fun getItemCount(): Int = layers?.size ?: 0

    inner class LayersViewHolder(private val binding: ItemLayerBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(layer: Layer?){
            binding.layer = layer
        }
    }
}