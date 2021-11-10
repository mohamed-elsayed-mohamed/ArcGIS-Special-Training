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
import com.esri.arcgisruntime.geometry.*
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.LayerList
import com.esri.arcgisruntime.mapping.view.*
import com.esri.arcgisruntime.portal.Portal
import com.esri.arcgisruntime.portal.PortalItem
import com.esri.arcgisruntime.symbology.SimpleFillSymbol
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import kotlin.math.roundToInt

/**
 * 1- Extracting and display features from web map service.
 *
 * 2- Show/Hide Layers
 *
 * 3- Calculate Area of Polygon
 *
 * References:-
 * - [Display a web map](https://developers.arcgis.com/android/maps-2d/tutorials/display-a-web-map/)
 * - [Feature Selection](https://developers.arcgis.com/android/kotlin/sample-code/feature-layer-selection/)
 * - [Show Attributes in Callout](https://developers.arcgis.com/android/java/sample-code/feature-layer-show-attributes/)
 * - [Draw a Polygon](https://developers.arcgis.com/android/maps-2d/tutorials/add-a-point-line-and-polygon/#add-a-polygon-graphic)
 * - [Format Coordinates](https://developers.arcgis.com/android/java/sample-code/format-coordinates/)
 * - [Calculate Area](https://developers.arcgis.com/android/api-reference/reference/com/esri/arcgisruntime/geometry/GeometryEngine.html#areaGeodetic(com.esri.arcgisruntime.geometry.Geometry,com.esri.arcgisruntime.geometry.AreaUnit,com.esri.arcgisruntime.geometry.GeodeticCurveType))
 */

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy{ ActivityMainBinding.inflate(layoutInflater) }
    private val mapView: MapView by lazy { binding.mapView }

    private val callout: Callout by lazy { mapView.callout }

    private val graphicsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }
    private val pointCollection = PointCollection(SpatialReferences.getWgs84())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.switchDraw.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                binding.fabDone.visibility = View.VISIBLE
            } else {
                binding.fabDone.visibility = View.INVISIBLE
                pointCollection.clear()
            }
        }

        binding.fabDone.setOnClickListener {
            if(pointCollection.isNotEmpty() && pointCollection.size > 2){
                val polygon = Polygon(pointCollection)
                val simpleFill = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.RED, null)
                graphicsOverlay.graphics.clear()
                graphicsOverlay.graphics.add(Graphic(polygon, simpleFill))

                val area = GeometryEngine.areaGeodetic(polygon, AreaUnit(AreaUnitId.SQUARE_KILOMETERS), GeodeticCurveType.GEODESIC)
                Toast.makeText(this, "Area: ${area.roundToInt()}", Toast.LENGTH_LONG).show()
            }

            pointCollection.clear()
        }

        setupMap()

        handleTouchListener()
    }

    private fun displayBottomSheet() {
        val layers: LayerList = mapView.map.operationalLayers
        val bottomSheetLayers = BottomSheetLayers()
        bottomSheetLayers.setLayers(layers)
        bottomSheetLayers.show(supportFragmentManager, BottomSheetLayers.TAG)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleTouchListener() {
        mapView.onTouchListener = object : DefaultMapViewOnTouchListener(this, mapView){
            override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {

                if(binding.switchDraw.isChecked){
                    if(pointCollection.isEmpty())
                        graphicsOverlay.graphics.clear()

                    val point = Point(motionEvent.x.toInt(), motionEvent.y.toInt())
                    val mapPoint = mapView.screenToLocation(point)
                    val simpleMarkerSymbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 10f)
                    graphicsOverlay.graphics.add(Graphic(mapPoint, simpleMarkerSymbol))

                    val toLatitudeLongitude = CoordinateFormatter.toLatitudeLongitude(mapPoint, CoordinateFormatter.LatitudeLongitudeFormat.DECIMAL_DEGREES, 4)
                    val latLang = CoordinateFormatter.fromLatitudeLongitude(toLatitudeLongitude, SpatialReferences.getWgs84())
                    pointCollection.add(latLang.x, latLang.y)

                    return true
                }

                val screenPoint = Point(motionEvent.x.roundToInt(), motionEvent.y.roundToInt())

                val identifyLayerResultFuture: ListenableFuture<List<IdentifyLayerResult>> = mapView.identifyLayersAsync(screenPoint, 0.0, false, 1)
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
            calloutContent.append("${attribute.key}: ${attribute.value ?: "N/A"}\n")
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
            setLines(7)
        }
    }

    private fun setupMap() {
        ArcGISRuntimeEnvironment.setApiKey(MapsConfigurations.API_KEY)
        val portal = Portal("https://www.arcgis.com", false)
        val portalItem = PortalItem(portal, "41281c51f9de45edaf1c8ed44bb10e30")
        mapView.map = ArcGISMap(portalItem)

        mapView.map.addDoneLoadingListener {
            if(mapView.map.loadStatus == LoadStatus.LOADED && mapView.map.operationalLayers.isNotEmpty())
                displayBottomSheet()

            mapView.graphicsOverlays.add(graphicsOverlay)
        }
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