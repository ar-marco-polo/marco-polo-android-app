package berlin.htw.augmentedreality.spatialaudio.activities

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
        val foundButton = findViewById(R.id.game_running__was_found_button)
        val abortButton = findViewById(R.id.game_running__abort_game_button)

        if (Game.amISeeking()) {
            foundButton.visibility = View.GONE
            explanation.text = getText(R.string.game_running_hider_text)
        } else {
            foundButton.visibility = View.VISIBLE
            explanation.text = getText(R.string.game_running_seeker_text)
        }

        foundButton.setOnClickListener {
            playerLost()
        }

        abortButton.setOnClickListener {
            abortGame()
        }

        Game.GameUpdateEvent on {
            when (it) {
                is Game.GameUpdateEvent.Movement -> Log.d(tag, "Somebody moved")
                is Game.GameUpdateEvent.Status -> Log.d(tag, "Got status event from server")
            }
        }
    }

    fun playerLost () {
        Log.d(tag, "Clicked 'I Was Found' button")
    }

    fun abortGame() {
        Log.d(tag, "Clicked abort button")
    }
}
