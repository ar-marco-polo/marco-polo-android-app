package berlin.htw.augmentedreality.spatialaudio

import android.app.Application
import android.util.Log

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("CUSTOM_APP", "Starting from custom app!")
        Game.setup(this)
        LocationUtils.setup(this)
    }
}