package com.alpha.arcgistraining.domain.constants

import com.esri.arcgisruntime.mapping.Viewpoint

object MapConstants {
    const val API_KEY =
        "AAPK744b9e5f33f0494bb075c1605fe36360t-VFExrsB6BtNN95JeUt1aoof_JBgVvDfG68cABb6xoKqNsgpzQ5wCKVRxjJ_IF_"

    const val LICENSE_KEY = "runtimelite,1000,rud3079300975,19-February-2018,E9PJD4SZ8LP7LMZ59172"

    const val ROUTING_SERVICE_URL =
        "https://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route"

    const val VECTOR_TILED_LAYER_URL =
        "https://www.arcgis.com/sharing/rest/content/items/dcbbba0edf094eaa81af19298b9c6247/resources/styles/root.json"

    val SAN_DIEGO_VIEW_POINT = Viewpoint(32.7157, -117.1611, 200000.0)

    const val ROUTE_TOTAL_TIME_TEXT = "totalTime"
    const val ROUTE_NAME_TIME_TEXT = "routeName"
    const val ROUTE_TOTAL_LENGTH_TEXT = "totalLength"

}