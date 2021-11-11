package com.alpha.regionfeatureextraction

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alpha.regionfeatureextraction.databinding.ItemLayerBinding
import com.esri.arcgisruntime.geometry.GeometryType
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.layers.Layer
import com.esri.arcgisruntime.mapping.LayerList
import com.esri.arcgisruntime.symbology.SimpleFillSymbol
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import com.esri.arcgisruntime.symbology.SimpleRenderer

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

            val featureLayer = layer as FeatureLayer

            binding.color1.setOnClickListener {
                when(featureLayer.featureTable.geometryType){
                    GeometryType.POINT, GeometryType.MULTIPOINT -> changePointColor(featureLayer, Color.RED)
                    GeometryType.POLYLINE -> changeLineColor(featureLayer, Color.RED)
                    GeometryType.POLYGON -> changePolygonColor(featureLayer, Color.RED)
                }
            }

            binding.color2.setOnClickListener {
                when(featureLayer.featureTable.geometryType){
                    GeometryType.POINT, GeometryType.MULTIPOINT -> changePointColor(featureLayer, Color.GREEN)
                    GeometryType.POLYLINE -> changeLineColor(featureLayer, Color.GREEN)
                    GeometryType.POLYGON -> changePolygonColor(featureLayer, Color.GREEN)
                }
            }

            binding.color3.setOnClickListener {
                when(featureLayer.featureTable.geometryType){
                    GeometryType.POINT, GeometryType.MULTIPOINT -> changePointColor(featureLayer, Color.BLUE)
                    GeometryType.POLYLINE -> changeLineColor(featureLayer, Color.BLUE)
                    GeometryType.POLYGON -> changePolygonColor(featureLayer, Color.BLUE)
                }
            }
        }

        private fun changePointColor(featureLayer: FeatureLayer, color: Int){

            val simpleMarkerSymbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.SQUARE, color, 4f)
            val simpleRenderer = SimpleRenderer(simpleMarkerSymbol)

            featureLayer.renderer = simpleRenderer
        }

        private fun changeLineColor(featureLayer: FeatureLayer, color: Int){

            val simpleLineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, color, 5f)
            val simpleRenderer = SimpleRenderer(simpleLineSymbol)

            featureLayer.renderer = simpleRenderer
        }

        private fun changePolygonColor(featureLayer: FeatureLayer, color: Int){
            val simpleOutLine = SimpleLineSymbol(SimpleLineSymbol.Style.DASH, Color.MAGENTA, 2f)
            val simpleFill = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, color, simpleOutLine)
            val simpleRenderer = SimpleRenderer(simpleFill)

            featureLayer.renderer = simpleRenderer
        }
    }
}