package berlin.htw.augmentedreality.spatialaudio

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.pm.PackageManager

class FullscreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)

        // DebugUtils.setup()
        Game.setup(this)
        LocationUtils.setup(this)

        val intent = intent
        val action = intent.action
        if (Intent.ACTION_VIEW == action) {
            // app was started from invitation link to join an exiting game
            val data = intent.data
            val gameName = data.getQueryParameter("g")
            Game.joinGame(gameName) { success ->
                if (success) { Game.start() }
            }
        } else {
            // normal app start
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
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            LocationUtils.LOCATION_REQUEST_CODE -> { LocationUtils.setupLocationListener() }
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
