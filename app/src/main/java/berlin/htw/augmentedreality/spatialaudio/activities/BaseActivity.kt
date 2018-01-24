package berlin.htw.augmentedreality.spatialaudio.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import berlin.htw.augmentedreality.spatialaudio.Game
import berlin.htw.augmentedreality.spatialaudio.LocationUtils

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Game.setup()
        LocationUtils.setup(this)
    }

}