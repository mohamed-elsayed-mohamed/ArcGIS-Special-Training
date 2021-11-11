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
 * 1-add feature layer using url
 * 2-find address within all the map or at specific extent
 * 3-Query Feature layer so only features within extent of a location are shown
 *
 * References:
 * - [Find address:]( https://developers.arcgis.com/android/kotlin/sample-code/find-address/)
 * - [Find address:]( https://developers.arcgis.com/android/java/sample-code/find-address/)
 * - [Feature Layer Query:]( https://developers.arcgis.com/android/java/sample-code/feature-layer-query/)
 * - [Query feature count and extent:]( https://developers.arcgis.com/net/android/sample-code/query-feature-count-and-extent/)
 * - [FeatureLayerClass:]( https://developers.arcgis.com/android/api-reference/reference/com/esri/arcgisruntime/layers/FeatureLayer.html#setFeaturesVisible(java.lang.Iterable,boolean))
 * - [FeatureLayerClass:]( https://developers.arcgis.com/android/api-reference/reference/com/esri/arcgisruntime/layers/FeatureLayer.html#setFeaturesVisible(java.lang.Iterable,boolean))
 * - [Add feature layer by url:]( https://developers.arcgis.com/android/layers/tutorials/add-a-feature-layer/)
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

    //listener on search view,when selecting item from suggestions then start geocode to find address
    //when writing text show suggestions of addresses
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

        //set parameters to get al features from feature table,when the geometry used is the map initial one
        //and spatialRelationship is intersecting -->means intersects features between geometry and feature layer
        val queryParams1 = QueryParameters()
        queryParams1.geometry = binding.mapView.map.initialViewpoint.targetGeometry
        queryParams1.isReturnGeometry = true
        queryParams1.spatialRelationship = QueryParameters.SpatialRelationship.INTERSECTS

        val queryListenable1 = featureTable.queryFeaturesAsync(queryParams1)

        queryListenable1.addDoneListener {
            if (queryListenable1.isDone) {
                val results1 = queryListenable1.get() as FeatureQueryResult

                val featuresList1 = ArrayList<Feature>()
                //iterate result of query and set them in list
                val resultIterator1 = results1.iterator()
                while (resultIterator1.hasNext()) {
                    val feature = resultIterator1.next()
                    featuresList1.add(feature)
                }
                //change visibility of all features into false
                featureLayer.setFeaturesVisible(featuresList1, false)


                //get the features within the geometry of the location we searched for
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
                        //iterate the query and set it in list = features within the geometry of search result
                        val resultIterator = results.iterator()
                        while (resultIterator.hasNext()) {
                            val feature = resultIterator.next()
                            featuresList.add(feature)
                        }
                        // set visibility of features within  to true
                        featureLayer.setFeaturesVisible(featuresList, true)
                    }
                }
            }
        }
    }


    private fun showSuggestions(newText: String) {

        var specificExtentGeometry: Geometry? = null
        val suggestedParameters = SuggestParameters()

        //when switch is checked then set suggestion parameters for our text only in the geometry we are in
        if (binding.switchSearch.isChecked) {
            specificExtentGeometry = binding.mapView.visibleArea.extent
            suggestedParameters.searchArea = specificExtentGeometry.extent
        }

        //load suggestions using locator task with parameter the suggestion params
        val suggestListenableFuture = locatorTask.suggestAsync(newText, suggestedParameters)

        suggestListenableFuture.addDoneListener {
            if (suggestListenableFuture.isDone) {
                val suggestions = suggestListenableFuture.get()

                //create the adapter we want to show suggestion in
                val cursor =
                    MatrixCursor(arrayOf(BaseColumns._ID, "address"))

                val adapter = SimpleCursorAdapter(
                    applicationContext, android.R.layout.simple_list_item_1, null,
                    arrayOf("address"),
                    intArrayOf(android.R.id.text1), 0
                )
                //get values of suggestions and set for adapter of our search view
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

                    //when clicking on suggestion take its value and set it up
                    override fun onSuggestionClick(p0: Int): Boolean {

                        binding.searchView.setQuery(suggestions[p0].label, true)

                        return true
                    }
                })
            }
        }

    }

    private fun gecodeSelectedAddress(query: String?) {
        //using locator task load the result of geocoding the address we clicked
        locatorTask.addDoneLoadingListener {

            if (locatorTask.loadStatus == LoadStatus.LOADED) {
                try {
                    val geocodeListenableFuture = locatorTask.geocodeAsync(query)

                    geocodeListenableFuture.addDoneListener {

                        val geocodeResult = geocodeListenableFuture.get()

                        if (geocodeResult.isNotEmpty()) {
                            geocodeResults = geocodeResult[0]
                            //add label for our geocoded result
                            addLabelOnSearchResult(geocodeResult[0])
                            //set view point into our result
                            binding.mapView.setViewpointGeometryAsync(geocodeResults.inputLocation.extent)
                            //start showing features near it
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
            //when clicking on extent button,reset features view and do to initial view point of feature layer
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