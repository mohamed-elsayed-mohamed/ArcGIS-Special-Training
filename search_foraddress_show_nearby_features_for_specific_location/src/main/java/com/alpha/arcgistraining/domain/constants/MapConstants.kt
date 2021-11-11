package com.alpha.arcgistraining.domain.constants

import com.esri.arcgisruntime.mapping.Viewpoint

object MapConstants {
    const val API_KEY =
        "AAPK744b9e5f33f0494bb075c1605fe36360t-VFExrsB6BtNN95JeUt1aoof_JBgVvDfG68cABb6xoKqNsgpzQ5wCKVRxjJ_IF_"

    const val LICENSE_KEY = "runtimelite,1000,rud3079300975,19-February-2018,E9PJD4SZ8LP7LMZ59172"

    const val LOCATOR_TASK_SERVICE_URL =
        "https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer"

    val NORTH_AMERICA_VIEW_POINT = Viewpoint(40.0, -100.0, 100000000.0)

    const val SERVICE_FEATURE_TABLE_URL =
        "https://services3.arcgis.com/GVgbJbqm8hXASVYi/arcgis/rest/services/Trailheads/FeatureServer/0"

}