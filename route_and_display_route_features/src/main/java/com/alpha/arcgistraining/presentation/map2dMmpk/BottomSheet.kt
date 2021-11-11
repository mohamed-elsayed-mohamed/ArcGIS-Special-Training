package com.alpha.arcgistraining.presentation.map2dMmpk

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.alpha.arcgistraining.databinding.BottomsheetBinding
import com.esri.arcgisruntime.tasks.networkanalysis.DirectionManeuver
import com.google.android.material.bottomsheet.BottomSheetBehavior

class BottomSheet(private var directionManeuvers: List<DirectionManeuver>) : Fragment() {


    private val binding by lazy { BottomsheetBinding.inflate(layoutInflater) }

    private var bottomSheetBehavior: BottomSheetBehavior<View> = BottomSheetBehavior()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.clBottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    setupBottomSheetStateOneThird()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })

        val directionsArray = ArrayList<String>()
        for (direction in directionManeuvers) {
            directionsArray.add(direction.directionText)
        }

        val adapter = ArrayAdapter(
            requireContext().applicationContext,
            R.layout.simple_list_item_1,
            directionsArray
        )
        binding.rvBottomSheet.adapter = adapter

        binding.rvBottomSheet.setOnItemClickListener { _, _, i, _ ->
            //on each item click cal function in main activity to change viewpoint and add graphic for maneuver
            (this.activity as MainActivity).listViewItemClicks(i)
        }
    }


    fun setupBottomSheetStateOneThird() {
        val params = binding.clBottomSheet.layoutParams
        params.height = (getScreenHeight() / 2.5).toInt()
        binding.clBottomSheet.layoutParams = params

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun getScreenHeight(): Int {
        return resources.displayMetrics.heightPixels
    }

}