package com.alpha.regionfeatureextraction

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alpha.regionfeatureextraction.databinding.ActivityMainBinding
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.concurrent.ListenableFuture
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.view.Callout
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.portal.Portal
import com.esri.arcgisruntime.portal.PortalItem
import kotlin.math.roundToInt

/**
 * 1- Extracting and display features from web map service.
 *
 *
 * References:-
 * - [Display a web map](https://developers.arcgis.com/android/maps-2d/tutorials/display-a-web-map/)
 * - https://developers.arcgis.com/android/kotlin/sample-code/feature-layer-selection/
 * - https://developers.arcgis.com/android/java/sample-code/feature-layer-show-attributes/
 */

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy{ ActivityMainBinding.inflate(layoutInflater) }
    private val mapView: MapView by lazy { binding.mapView }

    private val callout: Callout by lazy { mapView.callout }

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
                        if(callout.isShowing) callout.dismiss()
                        val listIdentifyLayerResult: List<IdentifyLayerResult> = identifyLayerResultFuture.get()
                        if(listIdentifyLayerResult.isNotEmpty()){
                            displayFeatures(listIdentifyLayerResult.first())
                        } else {
                            Toast.makeText(this@MainActivity, "Nothing", Toast.LENGTH_SHORT).show()
                        }
                    }catch (ex: Exception){
                        ex.printStackTrace()
                    }
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                if (callout.isShowing) callout.dismiss()
                return super.onDoubleTap(e)
            }
        }
    }

    private fun displayFeatures(identifyLayerResult: IdentifyLayerResult) {
        val attributes = identifyLayerResult.elements[0].attributes
        val calloutContent = getCalloutContent()

        for(attribute in attributes){
            calloutContent.append("Key: ${attribute.key}, Value: ${attribute.value ?: "N/A"}")
        }

        val envelope = identifyLayerResult.elements[0].geometry.extent
        mapView.setViewpointGeometryAsync(envelope, 200.0)
        callout.location = envelope.center
        callout.content = calloutContent
        callout.show()

    }

    private fun getCalloutContent(): TextView{
        return TextView(this).apply {
            setTextColor(Color.BLACK)
            isSingleLine = false
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
            movementMethod = ScrollingMovementMethod()
            setLines(5)
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