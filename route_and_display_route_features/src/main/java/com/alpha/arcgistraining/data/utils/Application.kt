package com.alpha.arcgistraining.data.utils

import android.app.Application

class Application : Application() {

    override fun onCreate() {
        super.onCreate()
        application = this
    }

    companion object {
        lateinit var application: Application
    }
}