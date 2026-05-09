package org.jiangstack.mytavern

import android.app.Application

class MyTavernApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
