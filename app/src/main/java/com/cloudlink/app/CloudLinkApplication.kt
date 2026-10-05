package com.cloudlink.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CloudLinkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
