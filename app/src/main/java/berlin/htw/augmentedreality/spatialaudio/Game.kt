package berlin.htw.augmentedreality.spatialaudio

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.media.MediaPlayer
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpPost
import java.util.*

object Game {
    data class Player (
        val id: String,
        val token: String?,
        var location: Location? = null
    )

    var player: Player? = null

    private val BASE_URL = "http://192.168.0.106:3000"

    private var activity: Activity? = null
    private var mediaPlayer: MediaPlayer? = null
    private var rotationSensor: Sensor? = null

    fun setup(activity: Activity) {
        this.activity = activity
        FuelManager.instance.basePath = BASE_URL
    }

    fun createNewGame(handler: (success: Boolean) -> Unit) {
        "/games".httpPost().responseJson { _, _, result ->
            val (data, error) = result
            val you = data?.obj()?.getJSONObject("you")
            val id = you?.getString("id")
            val token = you?.getString("token")

            if (error == null && id != null && token != null) {
                player = Player(id, token)
                handler(true)
            } else {
                handler(false)
            }
        }
    }

    fun start() {
        // TODO: maybe we can throw here or something to let the user retry
        val activity = activity ?: return

        // Initialize MediaPlayer
        mediaPlayer = MediaPlayer.create(activity, R.raw.freesounds_org__384468__frankum__vintage_elecro_pop_loop)
        mediaPlayer!!.setVolume(0.0f, 0.0f)
        mediaPlayer!!.isLooping = true
        mediaPlayer!!.start()

        // set up sensor listener
        // see also: https://source.android.com/devices/sensors/sensor-types#rotation_vector
        val sensorManager = (activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager)

        // rotation vector (= Accelerometer, Magnetometer, and Gyroscope)
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(RotationEventListener(), rotationSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private class RotationEventListener : SensorEventListener {
        override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        override fun onSensorChanged(event: SensorEvent) {
            // make shure game is set up with a player and a location
            val player = player ?: return
            val ownLocation = player.location ?: return

            val (x, y, z, w) = event.values
            val quaternion = floatArrayOf(w, x, y, z)

            // the android os assumes our original device rotation to be laid down flat on the ground, facing the
            // geomagnetic north pole; we position our ears on the top left corner and top right corner of this phone
            val origin = arrayOf(
                    floatArrayOf(-0.1f, 1.0f, 0.0f),
                    floatArrayOf(0.1f, 1.0f, 0.0f)
            )

            // now we rotate the original ears by our device position
            val ears = origin.map { ear -> VectorUtils.rotateByQuaternion(quaternion, ear) }

            val audioCurve = { x: Double -> if (x > Math.PI / 2) 0.0 else Math.pow(x - 2.16, 6.0) * 0.01 }
            val maxDistanceKm = 5

            val locationOfOtherPlayer = doubleArrayOf(52.520709, 13.409429) // Fernsehturm Berlin
            val ownLocationArray = doubleArrayOf(ownLocation.latitude, ownLocation.longitude)
            val directionToOtherPlayer = Geodesic.bearing(ownLocationArray, locationOfOtherPlayer)
            val distanceToOtherPlayer = Geodesic.distanceBetween(ownLocationArray, locationOfOtherPlayer)
            val vectorToOtherPlayer = Geodesic.bearingToVector3(directionToOtherPlayer)
            val distanceFactor = (maxDistanceKm - distanceToOtherPlayer) / maxDistanceKm

            DebugUtils.sendEarPositions(ears)

            // and calculate the angles between the ears and our sound position
            val volume = ears.map { ear ->
                val rad = VectorUtils.radiansBetween(ear, vectorToOtherPlayer)
                // rad is in range of [0, PI] audioCurve returns 0 above PI / 2
                distanceFactor * audioCurve(rad)
            }

            mediaPlayer?.setVolume(volume[0].toFloat(), volume[1].toFloat())
        }
    }
}