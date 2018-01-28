package berlin.htw.augmentedreality.spatialaudio

import android.util.Log
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket

object DebugUtils {
    val IS_ACTIVE = true
    val SERVER_URL = "http://192.168.0.100:3000"
    private var isInitialized = false
    private var webSocket: Socket? = null

    fun setup() {
        if (!IS_ACTIVE || isInitialized) return
        // Connect to webSocket to send data to debug view
        webSocket = IO.socket(SERVER_URL)
        webSocket!!
                .on(Socket.EVENT_CONNECT_ERROR, { e ->
                    Log.e("SOCKET", "Connection error %s".format(e))
                })
                .on(Socket.EVENT_CONNECT_TIMEOUT, { e ->
                    Log.e("SOCKET", "Connection timeout %s".format(e))
                })
                .on(Socket.EVENT_ERROR, { e ->
                    Log.e("SOCKET", "Connection error %s".format(e))
                })
                .on(Socket.EVENT_CONNECT, { e ->
                    Log.d("SOCKET", "Connection established %s".format(e))
                })
        webSocket!!.connect()
        isInitialized = true
    }

    fun sendEarPositions(ears: List<FloatArray>) {
        if (!IS_ACTIVE) return

        // send ear positions to debug server
        webSocket?.let { webSocket ->
            val jsonString = """
                    {
                        "ears": [${ears.map {
                            "[${it.joinToString(",")}]"
                        }.joinToString(",")}]
                    }
                    """
            webSocket.emit("rotation", jsonString)
        }
    }
}