package com.saifit.app

import android.app.Application
import com.saifit.app.di.AppContainer

class SaiFitApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
