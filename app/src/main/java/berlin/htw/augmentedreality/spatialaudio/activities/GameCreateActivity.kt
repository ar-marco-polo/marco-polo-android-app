package berlin.htw.augmentedreality.spatialaudio.activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.pm.PackageManager
import android.util.Log
import berlin.htw.augmentedreality.spatialaudio.Game
import berlin.htw.augmentedreality.spatialaudio.LocationUtils
import berlin.htw.augmentedreality.spatialaudio.R

class GameCreateActivity : AppCompatActivity() {

    val TAG = "GAME_CREATE_ACTIVITY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)

        // DebugUtils.setup()
        Game.setup(this)
        LocationUtils.setup(this)

        findViewById(R.id.game_creation_button).setOnClickListener {
            Log.d(TAG, "Clicked game creation button")
            Game.createNewGame { gameName ->
                if (gameName != null) {
                    Game.start()

                    // show share dialog to invite other player
                    val sendIntent = Intent()
                    val invitationLink = "${Game.BASE_URL}/join?g=$gameName"
                    sendIntent.action = Intent.ACTION_SEND
                    sendIntent.putExtra(Intent.EXTRA_TEXT, invitationLink)
                    sendIntent.type = "text/plain"
                    startActivity(sendIntent)
                } else {
                    // TODO: show error
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            LocationUtils.LOCATION_REQUEST_CODE -> {
                LocationUtils.setupLocationListener()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LocationUtils.ACCESS_FINE_LOCATION_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LocationUtils.setupLocationListener()
                }
            }
        }
    }
}
