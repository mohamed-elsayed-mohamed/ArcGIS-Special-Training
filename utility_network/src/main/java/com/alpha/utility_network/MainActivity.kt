package com.alpha.utility_network

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alpha.utility_network.databinding.ActivityMainBinding
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.mapping.view.MapView

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val mapView: MapView by lazy { binding.mapView }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupMap()
    }

    private fun setupMap(){
        ArcGISRuntimeEnvironment.setApiKey(MapsConfigurations.API_KEY)
    }
}