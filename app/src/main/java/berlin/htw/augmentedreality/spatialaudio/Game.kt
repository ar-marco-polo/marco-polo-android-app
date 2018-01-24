package berlin.htw.augmentedreality.spatialaudio

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
import berlin.htw.augmentedreality.spatialaudio.messages.PlayerLocation
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpPost
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import org.json.JSONObject

object Game {
    data class Player (
        val id: String,
        val token: String?,
        val isSeeking: Boolean,
        var location: Location? = null
    )

    data class GameData (
        val name: String,
        val me: Player,
        var other: Player?
    )

    sealed class GameUpdateEvent {
        companion object : Event<GameUpdateEvent>()

        class OwnLocationChanged(val location: Location) : GameUpdateEvent() {
            fun emit() = Companion.emit(this)
        }

        class OtherLocationChanged(val playerLocation: PlayerLocation) : GameUpdateEvent() {
            fun emit() = Companion.emit(this)
        }
    }

    val BASE_URL = "http://192.168.0.4:3000"

    private var game: GameData? = null
    private var mediaPlayer: MediaPlayer? = null
    private var rotationSensor: Sensor? = null
    private var webSocket: Socket? = null

    fun setup() {
        FuelManager.instance.basePath = BASE_URL
    }

    fun getOrCreateGame(handler: (game: GameData?) -> Unit) {
        if (game != null) return handler(game)

        "/games".httpPost().responseJson { _, _, result ->
            val (data, error) = result

            val json = data?.obj()
            val gameName = json?.getJSONObject("game")?.getString("name")
            val me = json?.getJSONObject("you")
            val id = me?.getString("id")
            val token = me?.getString("token")

            if (error == null && gameName != null && id != null && token != null) {
                val player = Player(id, token, true)
                game = GameData(gameName, player, null)
                handler(game)
            } else {
                handler(null)
            }
        }
    }

    fun joinGame(gameName: String, handler: (success: Boolean) -> Unit) {
        "/games/$gameName".httpPost().responseJson { _, _, result ->
            val (data, error) = result
            val json = data?.obj()
            val me = json?.getJSONObject("you")
            val id = me?.getString("id")
            val token = me?.getString("token")

            if (error == null && id != null && token != null) {
                val player = Player(id, token, false)
                game = GameData(gameName, player, null)
                handler(true)
            } else {
                handler(false)
            }
        }
    }

    fun amISeeking (): Boolean = game?.me?.isSeeking ?: false

    fun start(ctx: Context) {
        // TODO: maybe we can throw here or something to let the user retry when activity is missing
        val game = game ?: return
        val token = game.me.token ?: return

        // Initialize MediaPlayer
        mediaPlayer = MediaPlayer.create(ctx, R.raw.freesounds_org__384468__frankum__vintage_elecro_pop_loop)
        mediaPlayer!!.setVolume(0.0f, 0.0f)
        mediaPlayer!!.isLooping = true
        mediaPlayer!!.start()

        // set up sensor listener
        // see also: https://source.android.com/devices/sensors/sensor-types#rotation_vector
        val sensorManager = (ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager)

        // rotation vector (= Accelerometer, Magnetometer, and Gyroscope)
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(RotationEventListener(), rotationSensor, SensorManager.SENSOR_DELAY_NORMAL)

        webSocket = connectSocket(game.name, game.me.id, token)
        webSocket!!.on("location", { args ->
            val json = (args[0] as JSONObject).toString()
            val JSON = jacksonObjectMapper()
            val status: PlayerLocation = JSON.readValue(json)
            handleOtherLocationChange(status)
        })
    }

    fun handleOtherLocationChange(playerLocation: PlayerLocation) {
        val game = game ?: return

        val location = Location("spatial-audio")
        location.latitude = playerLocation.latitude
        location.longitude = playerLocation.longitude
        location.accuracy = playerLocation.accuracy

        if (game.other == null) {
            game.other = Player(playerLocation.playerId!!, null, !amISeeking(), location)
        } else {
            // TODO: check if player id matches
            game.other?.location = location
        }

        GameUpdateEvent.OtherLocationChanged(playerLocation).emit()
    }

    fun handleOwnLocationChnage(location: Location) {
        val game = game ?: return
        val webSocket = webSocket ?: return

        game.me.location = location
        val JSON = jacksonObjectMapper()
        val movement = PlayerLocation(location)
        webSocket.emit("location", JSON.writeValueAsBytes(movement))

        GameUpdateEvent.OwnLocationChanged(location).emit()
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
            // make shure game is set up with locations
            val game = game ?: return
            val ownLocation = game.me.location ?: return
            val otherLocation = game.other?.location ?: return

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

            val otherLocationArray = doubleArrayOf(otherLocation.latitude, otherLocation.longitude)
            val ownLocationArray = doubleArrayOf(ownLocation.latitude, ownLocation.longitude)
            val directionToOtherPlayer = Geodesic.bearing(ownLocationArray, otherLocationArray)
            val distanceToOtherPlayer = Geodesic.distanceBetween(ownLocationArray, otherLocationArray)
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
