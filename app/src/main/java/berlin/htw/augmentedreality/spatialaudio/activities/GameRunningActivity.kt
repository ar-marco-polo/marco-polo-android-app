package berlin.htw.augmentedreality.spatialaudio.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import berlin.htw.augmentedreality.spatialaudio.Game
import berlin.htw.augmentedreality.spatialaudio.R

class GameRunningActivity : BaseActivity() {

    private val tag = "GAME_RUNNING_ACTIVITY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_running)

        val explanation = findViewById(R.id.game_running__explanation_text) as TextView
        val gameOverButton = findViewById(R.id.game_running__game_over_button)
        val abortButton = findViewById(R.id.game_running__abort_game_button)
        val accuracy = findViewById(R.id.game_running__location_accuracy) as TextView

        if (Game.amISeeking()) {
            gameOverButton.visibility = View.GONE
            explanation.text = getText(R.string.game_running_seeker_text)
        } else {
            gameOverButton.visibility = View.VISIBLE
            explanation.text = getText(R.string.game_running_hider_text)
        }

        gameOverButton.setOnClickListener {
            gameOver()
        }

        abortButton.setOnClickListener {
            abortGame()
        }

        Game.GameUpdateEvent on {
            when (it) {
                is Game.GameUpdateEvent.OwnLocationChanged -> {
                    val loc = it.location
                    val label = getString(R.string.game_running_location_accuracy_label, loc.accuracy)
                    runOnUiThread { accuracy.text = label }
                }
                is Game.GameUpdateEvent.OtherLocationChanged -> Log.d(tag, "Got status event from server")
                is Game.GameUpdateEvent.GameOver -> {
                    if (it.won) {
                        displayCongratulation()
                    } else {
                        displayOtherPlayerAborted()
                    }
                }
            }
        }
    }

    fun gameOver() {
        Log.d(tag, "Clicked 'I Was Found' button")
        Game.gotCaught()
        displayNewGameScreen()
    }

    fun abortGame() {
        Log.d(tag, "Clicked abort button")
        Game.abort()
        displayNewGameScreen()
    }

    fun displayCongratulation() {
        AlertDialog.Builder(this)
                .setTitle("Congratulations!")
                .setMessage("You won. :)")
                .setOnDismissListener {
                    displayNewGameScreen()
                }
                .show()
    }

    fun displayOtherPlayerAborted() {
        AlertDialog.Builder(this)
                .setTitle("Game ended")
                .setMessage("The other player aborted the game.")
                .setOnDismissListener {
                    displayNewGameScreen()
                }
                .show()
    }

    fun displayNewGameScreen() {
        val createNewGame = Intent(this, GameCreateActivity::class.java)
        createNewGame.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
        startActivity(createNewGame)
        finish()
    }
}
