package com.alpha.arcgistraining.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.database.MatrixCursor
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.BaseColumns
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.SimpleCursorAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.alpha.arcgistraining.R
import com.alpha.arcgistraining.databinding.ActivityMainBinding
import com.alpha.arcgistraining.domain.constants.MapConstants
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult
import com.esri.arcgisruntime.tasks.geocode.LocatorTask
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

    private fun showSuggestions(newText: String) {

        val suggestListenableFuture = locatorTask.suggestAsync(newText)

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
                            addLabelOnSearchResult(geocodeResult[0])
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

        //set the mapView is our map to display it, with initial viewpoint as well as adding graphicOverlay on it
        binding.mapView.map = map
        binding.mapView.setViewpoint(MapConstants.NORTH_AMERICA_VIEW_POINT)
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


                    }
                    return true
                }
            }
    }

    private fun initializeLocatorTask() {
        locatorTask = LocatorTask(MapConstants.LOCATOR_TASK_SERVICE_URL)

        locatorTask.loadAsync()

//        locatorTask.addDoneLoadingListener {
//            if(locatorTask.loadStatus == LoadStatus.LOADED){
//
//            }
//        }
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