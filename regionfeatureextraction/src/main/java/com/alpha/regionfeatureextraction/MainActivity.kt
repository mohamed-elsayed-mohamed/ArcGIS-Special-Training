package com.alpha.regionfeatureextraction

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alpha.regionfeatureextraction.databinding.ActivityMainBinding
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.MobileMapPackage
import com.esri.arcgisruntime.mapping.view.MapView
import java.io.File
import java.io.FileNotFoundException

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy{ ActivityMainBinding.inflate(layoutInflater) }
    private val mapView: MapView by lazy { binding.mapView }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupMap()

        val mobileMapPackage = MobileMapPackage(getFileFromAssets(MapsConfigurations.OfflineMapbook)?.absolutePath)

        mobileMapPackage.loadAsync()
        mobileMapPackage.addDoneLoadingListener {
            if(mobileMapPackage.loadStatus == LoadStatus.LOADED && mobileMapPackage.maps.isNotEmpty()){
                mapView.map = mobileMapPackage.maps.first()
                val locator = mobileMapPackage.locatorTask

                if(locator == null)
                    Toast.makeText(this, "No Locator", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Error occurred while loading map!", Toast.LENGTH_LONG).show()
            }
        }
    }



    fun getFileFromAssets(fileName: String): File? {
        return try {
            File(this.cacheDir, fileName)
                .also {
                    it.outputStream()
                        .use { cache ->
                            this.assets.open(fileName)
                                .use { inputStream -> inputStream.copyTo(cache) }
                        }
                }
        } catch (exception: FileNotFoundException){
            null
        }

    }



    private fun setupMap() {
        ArcGISRuntimeEnvironment.setApiKey(MapsConfigurations.API_KEY)
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