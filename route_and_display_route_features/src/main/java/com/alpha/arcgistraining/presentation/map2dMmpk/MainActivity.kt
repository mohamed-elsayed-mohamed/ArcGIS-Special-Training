package com.alpha.arcgistraining.presentation.map2dMmpk

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
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
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import com.esri.arcgisruntime.tasks.networkanalysis.*
import kotlin.math.roundToInt

/*
references:
    find route:     https://developers.arcgis.com/android/kotlin/sample-code/find-route/
    RouteTaskClass: https://developers.arcgis.com/android/api-reference/reference/com/esri/arcgisruntime/tasks/networkanalysis/RouteTask.html
    RouteParametersClass: https://developers.arcgis.com/android/api-reference/reference/com/esri/arcgisruntime/tasks/networkanalysis/RouteParameters.html
    RouteClass:     https://developers.arcgis.com/android/api-reference/reference/com/esri/arcgisruntime/tasks/networkanalysis/Route.html
    RouteResultClass:   https://developers.arcgis.com/android/api-reference/reference/com/esri/arcgisruntime/tasks/networkanalysis/RouteResult.html


    offlineRouting using geodatabase: https://developers.arcgis.com/android/kotlin/sample-code/offline-routing/
 */
class MainActivity : AppCompatActivity() {

    private lateinit var directionManeuvers: MutableList<DirectionManeuver>

    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val routeStops: ArrayList<Stop> = arrayListOf()

    private val graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    private var routeGraphic = Graphic()

    private var graphicRoutePoint = Graphic()

    private lateinit var textToSpeech: TextToSpeech


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        ArcGISRuntimeEnvironment.setApiKey(MapConstants.API_KEY)

        setupMap()

        setMapTouchListener()

        initializeTextToSpeech()

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
                                setupDestinationRoute(
                                    Stop(binding.mapView.screenToLocation(screenPoint))
                                )
                                startRouteTask()
                            }
                            2 -> {
                                showCalloutsForRoute(
                                    routeGraphic,
                                    binding.mapView.screenToLocation(screenPoint)
                                )
                                routeStops.add(Stop(binding.mapView.screenToLocation(screenPoint)))
                            }
                            else -> {
                                routeStops.clear()
                                binding.mapView.callout.dismiss()
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

                    //setup graphic of route attributes with data for later usage
                    routeGraphic.attributes["totalTime"] = route.totalTime.toInt()
                    routeGraphic.attributes["routeName"] = route.routeName
                    routeGraphic.attributes["totalLength"] = route.totalLength.toInt()

                    directionManeuvers = route.directionManeuvers

                    //start showing bottom sheet containing maneuvers
                    showDirectionManeuvers(directionManeuvers)
                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    private fun showDirectionManeuvers(
        directionManeuvers: List<DirectionManeuver>
    ) {
        //start the bottom sheet passing with it the direction maneuvers
        val bottomSheetFragment = BottomSheet(directionManeuvers)
        this.supportFragmentManager.beginTransaction()
            .add(binding.flBottomSheet.id, bottomSheetFragment).commit()
    }


    fun listViewItemClicks(i: Int) {

        //if graphic is in graphics overlays then remove it so no conflict between points of shown maneuvers directions
        if (graphicsOverlay.graphics.contains(graphicRoutePoint)) {
            graphicsOverlay.graphics.remove(graphicRoutePoint)
        }
        //set a point for each maneuver when click on item is list view
        val simpleMarkerSymbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 10f)
        val simpleLineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.RED, 2f)
        simpleMarkerSymbol.outline = simpleLineSymbol

        //change viewpoint on this maneuver direction and add it to graphicsOverlays
        binding.mapView.setViewpointGeometryAsync(directionManeuvers[i].geometry)
        graphicRoutePoint = Graphic(directionManeuvers[i].geometry, simpleMarkerSymbol)
        graphicsOverlay.graphics.add(graphicRoutePoint)

        //speak when clicking
        speak(directionManeuvers[i].directionText)

    }

    //when clicking on item in list view speak the direction Text
    private fun speak(directionText: String) {
        if(this::textToSpeech.isInitialized) {
            textToSpeech.speak(directionText, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    //initializing text to speech
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this, TextToSpeech.OnInitListener {
            if (it != TextToSpeech.ERROR) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    textToSpeech.language = Resources.getSystem().configuration.locales[0]
                } else {
                    textToSpeech.language = Resources.getSystem().configuration.locale
                }
            }
        })
    }

    private fun showCalloutsForRoute(routeGraphic: Graphic, tapLocation: Point) {
        //show the callouts containing details about route from graphic of route
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