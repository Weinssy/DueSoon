package com.duesoon.app

import android.app.Application
import com.duesoon.app.di.AppContainer
import com.duesoon.app.di.DefaultAppContainer

class DueSoonApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
