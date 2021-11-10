package com.alpha.regionfeatureextraction

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alpha.regionfeatureextraction.databinding.LayersBottomSheetBinding
import com.esri.arcgisruntime.mapping.LayerList
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetLayers : BottomSheetDialogFragment() {
    private val binding: LayersBottomSheetBinding by lazy {
        LayersBottomSheetBinding.inflate(
            layoutInflater
        )
    }
    private val screenHeight: Int by lazy { Resources.getSystem().displayMetrics.heightPixels }

    private val layersAdapter: LayersAdapter = LayersAdapter()

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).apply {
            peekHeight = (screenHeight / 2.5).toInt()

            if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                val params = binding.root.layoutParams
                params.height = peekHeight
                binding.root.layoutParams = params
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.rvLayers.adapter = layersAdapter
        return binding.root
    }

    fun setLayers(layers: LayerList){
        layersAdapter.setData(layers)
    }

    companion object {
        const val TAG = "BottomSheetMap2DLayers"
    }
}