package berlin.htw.augmentedreality.spatialaudio

import android.app.Activity
import com.fasterxml.jackson.module.kotlin.*
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.media.MediaPlayer
import android.util.Log
import berlin.htw.augmentedreality.spatialaudio.messages.Authentication
import berlin.htw.augmentedreality.spatialaudio.messages.Movement
import com.fasterxml.jackson.core.JsonFactory
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpPost
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket

object Game {
    data class Player (
        val id: String,
        val token: String?,
        var location: Location? = null
    )

    val jsonFactory = JsonFactory()

    var player: Player? = null

    val BASE_URL = "http://192.168.0.106:3000"

    private var name: String? = null
    private var activity: Activity? = null
    private var mediaPlayer: MediaPlayer? = null
    private var rotationSensor: Sensor? = null
    private var webSocket: Socket? = null

    fun setup(activity: Context) {
        if (activity != null) return
        this.activity = activity
        FuelManager.instance.basePath = BASE_URL
    }

    fun createNewGame(handler: (gameName: String?) -> Unit) {
        "/games".httpPost().responseJson { _, _, result ->
            val (data, error) = result
            val json = data?.obj()
            val you = json?.getJSONObject("you")
            val gameName = json?.getJSONObject("game")?.getString("name")
            val id = you?.getString("id")
            val token = you?.getString("token")

            if (error == null && gameName != null && id != null && token != null) {
                player = Player(id, token)
                name = gameName
                handler(gameName)
            } else {
                handler(null)
            }
        }
    }

    fun joinGame(gameName: String?, handler: (success: Boolean) -> Unit) {
        "/games/$gameName".httpPost().responseJson { _, _, result ->
            val (data, error) = result
            val json = data?.obj()
            val you = json?.getJSONObject("you")
            val id = you?.getString("id")
            val token = you?.getString("token")

            if (error == null && id != null && token != null) {
                name = gameName
                player = Player(id, token)
                handler(true)
            } else {
                handler(false)
            }
        }
    }

    fun start() {
        // TODO: maybe we can throw here or something to let the user retry when activity is missing
        val activity = activity ?: return
        val player = player ?: return
        val token = player.token ?: return
        val gameName = name ?: return

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

        webSocket = connectSocket(gameName, player.id, token)
        webSocket!!.on("status", { status ->
            print(status)
        })
    }

    fun handleMovement(location: Location) {
        val player = player ?: return
        val webSocket = webSocket ?: return

        player.location = location
        val JSON = jacksonObjectMapper()
        val movement = Movement(location)
        webSocket.emit("movement", JSON.writeValueAsBytes(movement))
    }

    private fun connectSocket(gameName: String, playerId: String, token: String): Socket {
        val socket = IO.socket("$BASE_URL")
        socket
                .on(Socket.EVENT_CONNECT_ERROR, { e ->
                    Log.e("SOCKET", "Connection error %s".format(e))
                })
                .on(Socket.EVENT_CONNECT_TIMEOUT, { e ->
                    Log.e("SOCKET", "Connection timeout %s".format(e))
                })
                .on(Socket.EVENT_ERROR, { e ->
                    Log.e("SOCKET", "Connection error %s".format(e))
                })
                .on(Socket.EVENT_CONNECT, { _ ->
                    val JSON = jacksonObjectMapper()
                    val authentication = Authentication(gameName, playerId, token)
                    socket.emit("auth", JSON.writeValueAsBytes(authentication))
                })
        socket.connect()
        return  socket
    }

    private class RotationEventListener : SensorEventListener {
        override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        override fun onSensorChanged(event: SensorEvent) {
            // make shure game is set up with a player and a location
            val ownLocation = player?.location ?: return

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