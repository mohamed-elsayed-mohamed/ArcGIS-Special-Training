package com.alpha.arcgistraining.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.database.MatrixCursor
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.BaseColumns
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.SimpleCursorAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.alpha.arcgistraining.R
import com.alpha.arcgistraining.databinding.ActivityMainBinding
import com.alpha.arcgistraining.domain.constants.MapConstants
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.data.Feature
import com.esri.arcgisruntime.data.FeatureQueryResult
import com.esri.arcgisruntime.data.QueryParameters
import com.esri.arcgisruntime.data.ServiceFeatureTable
import com.esri.arcgisruntime.geometry.Geometry
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult
import com.esri.arcgisruntime.tasks.geocode.LocatorTask
import com.esri.arcgisruntime.tasks.geocode.SuggestParameters
import kotlin.math.roundToInt


/**
 * 1-find address
 *
 * References:-
 * - [Find address:]( https://developers.arcgis.com/android/kotlin/sample-code/find-address/)
 */

class MainActivity : AppCompatActivity() {
    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    private lateinit var locatorTask: LocatorTask

    private lateinit var featureLayerExtent: Geometry
    private lateinit var geocodeResults: GeocodeResult
    private lateinit var featureLayer: FeatureLayer
    private lateinit var featureTable: ServiceFeatureTable


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        ArcGISRuntimeEnvironment.setApiKey(MapConstants.API_KEY)

        setupMap()
        initializeLocatorTask()

        setMapTouchListener()

        setListeners()

    }

    private fun setListeners() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {

                gecodeSelectedAddress(query)



                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty()) {
                    showSuggestions(newText)
                }
                return true
            }
        })
    }

    private fun showFeaturesWithin(geocodeResults: GeocodeResult) {

        val queryParams1 = QueryParameters()
        queryParams1.geometry = binding.mapView.map.initialViewpoint.targetGeometry
        queryParams1.isReturnGeometry = true
        queryParams1.spatialRelationship = QueryParameters.SpatialRelationship.INTERSECTS

        val queryListenable1 = featureTable.queryFeaturesAsync(queryParams1)

        queryListenable1.addDoneListener {
            if (queryListenable1.isDone) {
                val results1 = queryListenable1.get() as FeatureQueryResult

                val featuresList1 = ArrayList<Feature>()

                val resultIterator1 = results1.iterator()
                while (resultIterator1.hasNext()) {
                    val feature = resultIterator1.next()
                    featuresList1.add(feature)
                }
                featureLayer.setFeaturesVisible(featuresList1, false)

                val resultViewPoint = Viewpoint(geocodeResults.inputLocation, 10000.0)
                binding.mapView.setViewpoint(resultViewPoint)
                val resultViewPointGeometry =
                    binding.mapView.getCurrentViewpoint(Viewpoint.Type.BOUNDING_GEOMETRY).targetGeometry

                val queryParams = QueryParameters()
                queryParams.geometry = resultViewPointGeometry
                queryParams.isReturnGeometry = true
                queryParams.spatialRelationship = QueryParameters.SpatialRelationship.INTERSECTS

                val queryListenable = featureTable.queryFeaturesAsync(queryParams)

                queryListenable.addDoneListener {
                    if (queryListenable.isDone) {
                        val results = queryListenable.get() as FeatureQueryResult

                        val featuresList = ArrayList<Feature>()

                        val resultIterator = results.iterator()
                        while (resultIterator.hasNext()) {
                            val feature = resultIterator.next()
                            featuresList.add(feature)
                        }
                        featureLayer.setFeaturesVisible(featuresList, true)
                    }
                }
            }
        }
    }


    private fun showSuggestions(newText: String) {

        var specificExtentGeometry: Geometry? = null
        val suggestedParameters = SuggestParameters()


        if (binding.switchSearch.isChecked) {
            specificExtentGeometry = binding.mapView.visibleArea.extent
            suggestedParameters.searchArea = specificExtentGeometry.extent
        }

        val suggestListenableFuture = locatorTask.suggestAsync(newText, suggestedParameters)

        suggestListenableFuture.addDoneListener {
            if (suggestListenableFuture.isDone) {
                val suggestions = suggestListenableFuture.get()

                val cursor =
                    MatrixCursor(arrayOf(BaseColumns._ID, "address"))

                val adapter = SimpleCursorAdapter(
                    applicationContext, android.R.layout.simple_list_item_1, null,
                    arrayOf("address"),
                    intArrayOf(android.R.id.text1), 0
                )

                for ((key, result) in suggestions.withIndex()) {
                    cursor.addRow(arrayOf(key, result.label))
                }

                adapter.changeCursor(cursor)

                binding.searchView.suggestionsAdapter = adapter


                binding.searchView.setOnSuggestionListener(object :
                    SearchView.OnSuggestionListener {
                    override fun onSuggestionSelect(p0: Int): Boolean {
                        return false
                    }

                    override fun onSuggestionClick(p0: Int): Boolean {

                        binding.searchView.setQuery(suggestions[p0].label, true)

                        return true
                    }
                })
            }
        }

    }

    private fun gecodeSelectedAddress(query: String?) {

        locatorTask.addDoneLoadingListener {

            if (locatorTask.loadStatus == LoadStatus.LOADED) {
                try {
                    val geocodeListenableFuture = locatorTask.geocodeAsync(query)

                    geocodeListenableFuture.addDoneListener {

                        val geocodeResult = geocodeListenableFuture.get()

                        if (geocodeResult.isNotEmpty()) {
                            geocodeResults = geocodeResult[0]
                            addLabelOnSearchResult(geocodeResult[0])

                            binding.mapView.setViewpointGeometryAsync(geocodeResults.inputLocation.extent)

                            showFeaturesWithin(geocodeResults)
                        }

                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }

            } else {
                locatorTask.retryLoadAsync()
            }
        }

    }

    private fun addLabelOnSearchResult(geocodeResult: GeocodeResult?) {

        graphicsOverlay.graphics.clear()

        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            binding.root.windowToken, 0
        )

        val searchResultIcon =
            ActivityCompat.getDrawable(
                applicationContext,
                R.drawable.ic_source_route
            ) as BitmapDrawable

        val pictureMarkerSymbol = PictureMarkerSymbol.createAsync(searchResultIcon).get()

        pictureMarkerSymbol.addDoneLoadingListener {
            if (pictureMarkerSymbol.loadStatus == LoadStatus.LOADED) {

                pictureMarkerSymbol.width = 25f
                pictureMarkerSymbol.height = 25f

                binding.mapView.setViewpointGeometryAsync(geocodeResult!!.extent)
                val pointGraphic = Graphic(geocodeResult.inputLocation, pictureMarkerSymbol)
                graphicsOverlay.graphics.add(pointGraphic)
            }
        }


    }

    private fun setupMap() {

        //create the map with base map ARCGIS_STREETS
        val map = ArcGISMap(BasemapStyle.ARCGIS_STREETS)

        // create the service feature table
        featureTable =
            ServiceFeatureTable(MapConstants.SERVICE_FEATURE_TABLE_URL)
        featureTable.loadAsync()

        featureTable.addDoneLoadingListener {
            if (featureTable.loadStatus == LoadStatus.LOADED) {
                // create the feature layer using the service feature table
                featureLayer = FeatureLayer(featureTable)
                featureLayer.loadAsync()
                featureLayer.addDoneLoadingListener {
                    if (featureLayer.loadStatus == LoadStatus.LOADED) {
                        map.operationalLayers.add(featureLayer)

                        featureLayerExtent = featureLayer.fullExtent

                        //set the mapView is our map to display it, with initial viewpoint as well as adding graphicOverlay on it
                        binding.mapView.map = map
                        binding.mapView.setViewpointGeometryAsync(featureLayer.fullExtent, 10.0)
                        binding.mapView.graphicsOverlays.add(graphicsOverlay)
                    }
                }
            }
        }

    }

    fun goToFeatureLayer(view: View) {
        if (this::featureLayerExtent.isInitialized) {
            binding.mapView.setViewpointGeometryAsync(featureLayerExtent, 10.0)
            featureLayer.resetFeaturesVisible()
        }
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
                    }
                    return true
                }
            }
    }

    private fun initializeLocatorTask() {
        locatorTask = LocatorTask(MapConstants.LOCATOR_TASK_SERVICE_URL)

        locatorTask.loadAsync()

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