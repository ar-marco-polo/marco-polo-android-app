package berlin.htw.augmentedreality.spatialaudio

import com.fasterxml.jackson.module.kotlin.*
import android.content.Context
import android.hardware.*
import android.location.Location
import android.media.MediaPlayer
import android.util.Log
import berlin.htw.augmentedreality.spatialaudio.messages.Authentication
import berlin.htw.augmentedreality.spatialaudio.messages.PlayerLocation
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.interceptors.cUrlLoggingRequestInterceptor
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

        class RotationChanged(val game: GameData) : GameUpdateEvent() {
            fun emit() = Companion.emit(this)
        }

        class OtherPlayerJoined : GameUpdateEvent() {
            fun emit() = Companion.emit(this)
        }

        class GameOver(val won: Boolean) : GameUpdateEvent() {
            fun emit() = Companion.emit(this)
        }
    }

    val BASE_URL = "http://159.89.110.19:3000"

    private var isInitialized = false
    private var game: GameData? = null
    private var trackingMediaPlayer: MediaPlayer? = null
    private var noiseMediaPlayer: MediaPlayer? = null
    private var webSocket: Socket? = null

    fun setup() {
        if (isInitialized) return
        FuelManager.instance.basePath = BASE_URL
        FuelManager.instance.addRequestInterceptor(cUrlLoggingRequestInterceptor())
        isInitialized = true
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
        trackingMediaPlayer = MediaPlayer.create(ctx, R.raw.freesounds_org__384468__frankum__vintage_elecro_pop_loop)
        trackingMediaPlayer!!.setVolume(0.0f, 0.0f)
        trackingMediaPlayer!!.isLooping = true
        trackingMediaPlayer!!.start()

        noiseMediaPlayer = MediaPlayer.create(ctx, R.raw.freesounds_org__35291__jace__continuous_static)
        noiseMediaPlayer!!.setVolume(0.0f, 0.0f)
        noiseMediaPlayer!!.isLooping = true
        noiseMediaPlayer!!.start()

        webSocket = connectSocket(game.name, game.me.id, token)

        webSocket!!
                .on("join", {
                    GameUpdateEvent.OtherPlayerJoined().emit()
                })
                .on("gotCaught", {
                    GameUpdateEvent.GameOver(won = true).emit()
                })
                .on("abort", {
                    GameUpdateEvent.GameOver(won = false).emit()
                })
                .on("location", { args ->
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
            game.other = Player(playerLocation.id!!, null, !amISeeking(), location)
        } else {
            // TODO: check if player id matches
            game.other?.location = location
        }

        GameUpdateEvent.OtherLocationChanged(playerLocation).emit()
    }

    fun handleOwnLocationChange(location: Location) {
        val game = game ?: return
        val webSocket = webSocket ?: return

        game.me.location = location
        val JSON = jacksonObjectMapper()
        val movement = PlayerLocation(location)
        webSocket.emit("location", JSON.writeValueAsBytes(movement))

        GameUpdateEvent.OwnLocationChanged(location).emit()
    }

    fun handleRotationChange(ears: List<FloatArray>) {
        // make shure game is set up with locations
        val game = Game.game ?: return
        val ownLocation = game.me.location ?: return
        val otherLocation = game.other?.location

        val maxNoise = 0.07f
        val maxDistance = 1000

        if (otherLocation == null) {
            Game.noiseMediaPlayer?.setVolume(maxNoise, maxNoise)
            return
        }

        val audioCurve = { x: Double -> if (x > Math.PI / 2) 0.0 else Math.pow(x - 2.16, 6.0) * 0.01 }

        val otherLocationArray = doubleArrayOf(otherLocation.latitude, otherLocation.longitude)
        val ownLocationArray = doubleArrayOf(ownLocation.latitude, ownLocation.longitude)
        val directionToOtherPlayer = Geodesic.bearing(ownLocationArray, otherLocationArray)
        val distanceToOtherPlayer = ownLocation.distanceTo(otherLocation)
        val vectorToOtherPlayer = Geodesic.bearingToVector3(directionToOtherPlayer)
        val distanceFactor = (maxDistance - distanceToOtherPlayer) / maxDistance
        val combinedAccuracy = ownLocation.accuracy + otherLocation.accuracy

        val noise = Math.min(combinedAccuracy / (distanceToOtherPlayer.toDouble() + 0.001), 1.0)
        val noiseVolume = (Math.pow(noise, 2.0) * maxNoise).toFloat()

        // and calculate the angles between the ears and our sound position
        val volume = ears.map { ear ->
            val rad = VectorUtils.radiansBetween(ear, vectorToOtherPlayer)
            val n = noise * Math.PI
            // rad is in range of [0, PI] audioCurve returns 0 above PI / 2
            distanceFactor * audioCurve(rad - n)
        }

        Game.trackingMediaPlayer?.setVolume(volume[0].toFloat(), volume[1].toFloat())
        Game.noiseMediaPlayer?.setVolume(noiseVolume, noiseVolume)

        DebugUtils.sendPositions(ears, vectorToOtherPlayer, 1 - distanceFactor)
        Game.GameUpdateEvent.RotationChanged(game).emit()
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

    fun teardown() {
        game = null
        trackingMediaPlayer?.stop()
        noiseMediaPlayer?.stop()
        webSocket?.disconnect()
        webSocket = null
    }

    fun abort() {
        webSocket!!.emit("abort")
        teardown()
    }

    fun gotCaught() {
        webSocket!!.emit("gotCaught")
        teardown()
    }
}
