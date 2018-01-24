package berlin.htw.augmentedreality.spatialaudio.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import berlin.htw.augmentedreality.spatialaudio.Game
import berlin.htw.augmentedreality.spatialaudio.R

class GameJoinActivity : BaseActivity() {

    val tag = "GAME_JOIN_ACTIVITY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_join)

        // app was started from invitation link to join an exiting game
        val data = intent.data
        val gameName = data.pathSegments.last()

        findViewById(R.id.game_join_button).setOnClickListener {
            Log.d(tag, "Clicked game join button")
            Game.joinGame(gameName) { success ->
                if (success) {
                    Game.start(this)
                    val gameRunning = Intent(this, GameRunningActivity::class.java)
                    gameRunning.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
                    startActivity(gameRunning)
                    finish()
                } else {
                    Log.e(tag, "Could not join game!!")
                    Toast.makeText(this, "Could not join game", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
