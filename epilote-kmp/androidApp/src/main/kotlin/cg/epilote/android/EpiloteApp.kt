package cg.epilote.android

import android.app.Application
import cg.epilote.android.di.AppContainer
import cg.epilote.shared.platform.initCouchbaseLite

class EpiloteApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        initCouchbaseLite(this)
        container = AppContainer(this)
    }
}
