package com.alpha.arcgistraining.presentation.map2dMmpk

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.alpha.arcgistraining.R
import com.alpha.arcgistraining.databinding.ActivityMainBinding
import com.alpha.arcgistraining.domain.constants.MapConstants
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.layers.ArcGISVectorTiledLayer
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import com.esri.arcgisruntime.tasks.networkanalysis.Route
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask
import com.esri.arcgisruntime.tasks.networkanalysis.Stop
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val routeStops: ArrayList<Stop> = arrayListOf()

    private val graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    private var routeGraphic = Graphic()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

//        ArcGISRuntimeEnvironment.setLicense(MapConstants.LICENSE_KEY)
        ArcGISRuntimeEnvironment.setApiKey(MapConstants.API_KEY)

        setupMap()

        setMapTouchListener()

    }

    private fun setupMap() {
        //create a ArcGISVectorTiledLayer using vector tiled layer from a service url
        val vectorTileLayer = ArcGISVectorTiledLayer(MapConstants.VECTOR_TILED_LAYER_URL)

        //create a base map using this vectorTileLayer
        val basemap = Basemap(vectorTileLayer)

        //create the map with base map is our vector tile layer
        val map = ArcGISMap(basemap)

        //set the mapView is our map to display it, with initial viewpoint as well as adding graphicOverlay on it
        binding.mapView.map = map
        binding.mapView.setViewpoint(MapConstants.SAN_DIEGO_VIEW_POINT)
        binding.mapView.graphicsOverlays.add(graphicsOverlay)

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setMapTouchListener() {
        binding.mapView.onTouchListener =
            object :
                DefaultMapViewOnTouchListener(this, binding.mapView) {

                override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                    e?.let {
                        val screenPoint =
                            android.graphics.Point(it.x.roundToInt(), it.y.roundToInt())

                        when (routeStops.size) {
                            0 -> {
                                setupSourceRoute(
                                    Stop(binding.mapView.screenToLocation(screenPoint))
                                )
                            }
                            1 -> {
                                setupSourceRoute(
                                    Stop(binding.mapView.screenToLocation(screenPoint))
                                )
                                startRouteTask()
                            }
                            2 -> {
                                showCalloutsForRoute(
                                    routeGraphic,
                                    binding.mapView.screenToLocation(screenPoint)
                                )
                            }
                            else -> {
                                routeStops.clear()
                                graphicsOverlay.graphics.clear()
                            }
                        }

                        Log.d(TAG, "screenPint : $screenPoint")
                    }
                    return true
                }
            }
    }

    private fun setupSourceRoute(sourceStop: Stop) {
        //get source icon from drawable
        val sourceIcon = ActivityCompat.getDrawable(
            applicationContext,
            R.drawable.ic_source_route
        ) as BitmapDrawable

        //start loading this icon to a pictureMarkerSymbol
        val pictureMarkerSymbol = PictureMarkerSymbol.createAsync(sourceIcon).get()

        pictureMarkerSymbol.addDoneLoadingListener {
            if (pictureMarkerSymbol.loadStatus == LoadStatus.LOADED) {

                //when loaded set its size
                pictureMarkerSymbol.width = 25f
                pictureMarkerSymbol.height = 25f

                //set the graphic source point and its symbol
                val sourcePointGraphic = Graphic(
                    sourceStop.geometry, pictureMarkerSymbol
                )

                //add this point into graphics
                graphicsOverlay.graphics.add(sourcePointGraphic)
                routeStops.add(sourceStop)
            }
        }
    }


    private fun setupDestinationRoute(destinationStop: Stop) {
        //get destination icon from drawable
        val destinationIcon = ActivityCompat.getDrawable(
            applicationContext,
            R.drawable.ic_source_route
        ) as BitmapDrawable

        //start loading this icon to a pictureMarkerSymbol
        val pictureMarkerSymbol = PictureMarkerSymbol.createAsync(destinationIcon).get()

        pictureMarkerSymbol.addDoneLoadingListener {
            if (pictureMarkerSymbol.loadStatus == LoadStatus.LOADED) {

                //when loaded set its size
                pictureMarkerSymbol.width = 25f
                pictureMarkerSymbol.height = 25f

                //set the graphic destination point and its symbol
                val destinationPointGraphic = Graphic(destinationStop.geometry, pictureMarkerSymbol)

                //add this point into graphics
                graphicsOverlay.graphics.add(destinationPointGraphic)
                routeStops.add(destinationStop)
            }
        }
    }

    private fun startRouteTask() {

        //create route task instance with the routing service url
        val routeTask = RouteTask(this, MapConstants.ROUTING_SERVICE_URL)

        routeTask.loadAsync()

        routeTask.addDoneLoadingListener {
            //create a set of default parameters for solving a route with this route task.
            val routeParametersFuture = routeTask.createDefaultParametersAsync()

            routeParametersFuture.addDoneListener {
                try {
                    if (routeParametersFuture.isDone) {
                        //if it's loaded then get the RouteParameters
                        val routeParams = routeParametersFuture.get()

                        val routeStops = routeStops

                        routeParams.apply {
                            //Sets the stops to visit in the result route.
                            setStops(routeStops)

                            //Gets whether to return direction maneuvers in the result.
                            isReturnDirections = true
                        }

                        solveTheRoute(routeTask, routeParams)

                        Log.d(TAG, routeParams.isReturnRoutes.toString())
                        Log.d(TAG, routeParams.isReturnStops.toString())
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

    }

    private fun solveTheRoute(routeTask: RouteTask, routeParams: RouteParameters?) {

        try {
            // solve (calculate) the route based on the provided route parameters and get the result = RouteResult
            val routeResultFuture = routeTask.solveRouteAsync(routeParams)

            routeResultFuture.addDoneListener {
                if (routeResultFuture.isDone) {//if route solved

                    val result = routeResultFuture.get()
                    val route = result.routes[0] as Route   //get first route from routes list

                    //create a line symbol for the route
                    val routeSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 2f)

                    //add the route as a graphic to map
                    routeGraphic = Graphic(route.routeGeometry, routeSymbol)
                    graphicsOverlay.graphics.add(routeGraphic)

                    //change mapView visibility on the route
                    binding.mapView.setViewpointGeometryAsync(route.routeGeometry, 50.0)

                    routeGraphic.attributes["totalTime"] = route.totalTime.toInt()
                    routeGraphic.attributes["routeName"] = route.routeName
                    routeGraphic.attributes["totalLength"] = route.totalLength.toInt()


                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    private fun showCalloutsForRoute(routeGraphic: Graphic, tapLocation: Point) {

        val calloutsTextView = layoutInflater.inflate(R.layout.callout, null) as TextView
        var routeInfo = "Route Name = " + routeGraphic.attributes["routeName"].toString() + "\n"
        routeInfo += "Total Time = " + routeGraphic.attributes["totalTime"].toString() + " minutes" + "\n"
        routeInfo += "Total Length = " + routeGraphic.attributes["totalLength"].toString() + " meters"
        calloutsTextView.text = routeInfo

        val callouts = binding.mapView.callout
        callouts.location = tapLocation
        callouts.content = calloutsTextView
        callouts.show()

    }

    companion object {
        const val TAG = "MAIN_TAG"
    }

    override fun onPause() {
        binding.mapView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onDestroy() {
        binding.mapView.dispose()
        super.onDestroy()
    }
}