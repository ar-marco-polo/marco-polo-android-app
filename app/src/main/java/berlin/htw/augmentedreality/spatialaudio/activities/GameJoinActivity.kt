package berlin.htw.augmentedreality.spatialaudio.activities

import android.os.Bundle
import android.widget.Toast
import berlin.htw.augmentedreality.spatialaudio.Game
import berlin.htw.augmentedreality.spatialaudio.R

class GameJoinActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_join)

        // app was started from invitation link to join an exiting game
        val data = intent.data
        val gameName = data.pathSegments.last()
        Game.joinGame(gameName) { success ->
            if (success) {
                Game.start()
            } else {
                Toast.makeText(this, "Could not join game", Toast.LENGTH_LONG).show()
            }
        }
    }
}
