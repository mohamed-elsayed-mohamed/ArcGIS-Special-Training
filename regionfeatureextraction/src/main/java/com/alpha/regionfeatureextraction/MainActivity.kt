package com.alpha.regionfeatureextraction

import android.annotation.SuppressLint
import android.graphics.Point
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alpha.regionfeatureextraction.databinding.ActivityMainBinding
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.concurrent.ListenableFuture
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.MobileMapPackage
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.portal.Portal
import com.esri.arcgisruntime.portal.PortalItem
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy{ ActivityMainBinding.inflate(layoutInflater) }
    private val mapView: MapView by lazy { binding.mapView }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupMap()

        handleTouchListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleTouchListener() {
        mapView.onTouchListener = object : DefaultMapViewOnTouchListener(this, mapView){
            override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {
                val screenPoint = Point(motionEvent.x.roundToInt(), motionEvent.y.roundToInt())

                val identifyLayerResultFuture: ListenableFuture<List<IdentifyLayerResult>> = mapView.identifyLayersAsync(screenPoint, 0.0, false)
                identifyLayerResultFuture.addDoneListener {
                    try {
                        val listIdentifyLayerResult: List<IdentifyLayerResult> = identifyLayerResultFuture.get()
                        if(listIdentifyLayerResult.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "Hello", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Nothing", Toast.LENGTH_SHORT).show()
                        }
                    }catch (ex: Exception){
                        ex.printStackTrace()
                    }
                }

                return true
            }
        }
    }


    private fun setupMap() {
        ArcGISRuntimeEnvironment.setApiKey(MapsConfigurations.API_KEY)
        val portal = Portal("https://www.arcgis.com", false)
        val portalItem = PortalItem(portal, "41281c51f9de45edaf1c8ed44bb10e30")
        mapView.map = ArcGISMap(portalItem)
    }

    override fun onPause() {
        super.onPause()
        mapView.pause()
    }

    override fun onResume() {
        super.onResume()
        mapView.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.dispose()
    }
}