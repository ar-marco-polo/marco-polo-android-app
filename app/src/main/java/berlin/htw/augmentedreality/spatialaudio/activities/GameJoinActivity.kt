package berlin.htw.augmentedreality.spatialaudio.activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import berlin.htw.augmentedreality.spatialaudio.Game
import berlin.htw.augmentedreality.spatialaudio.R

class GameJoinActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dummy)

        checkForJoinIntent()
    }

    fun checkForJoinIntent () {
        if (intent.action == Intent.ACTION_VIEW) {
            // app was started from invitation link to join an exiting game
            val data = intent.data
            val gameName = data.getQueryParameter("g")
            Game.joinGame(gameName) { success ->
                if (success) {
                    Game.start()
                }
            }
        }
    }
}
