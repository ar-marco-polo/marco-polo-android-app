package berlin.htw.augmentedreality.spatialaudio.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import berlin.htw.augmentedreality.spatialaudio.DebugUtils
import berlin.htw.augmentedreality.spatialaudio.Game
import berlin.htw.augmentedreality.spatialaudio.LocationUtils
import berlin.htw.augmentedreality.spatialaudio.RotationUtils

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocationUtils.setup(this)
        RotationUtils.setup(this)
        Game.setup()
        DebugUtils.setup()
    }

}