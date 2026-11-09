package dev.erolit.reader

import android.app.Application
import coil.Coil

class ErolitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Coil.setImageLoader(container.imageLoader)
    }
}
