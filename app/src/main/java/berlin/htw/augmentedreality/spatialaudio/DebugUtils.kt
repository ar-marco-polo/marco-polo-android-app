package berlin.htw.augmentedreality.spatialaudio

import android.util.Log
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket

object DebugUtils {
    val SERVER_URL = "http://141.45.208.185:3000"
    private var webSocket: Socket? = null

    fun setup() {
        // Connect to webSocket to send data to debug view
        webSocket = IO.socket(SERVER_URL)
        webSocket!!
                .on(Socket.EVENT_CONNECT_ERROR, { e ->
                    Log.e("SOCKET", "Connection error %s".format(e))
                })
                .on(Socket.EVENT_CONNECT_TIMEOUT, { e ->
                    Log.e("SOCKET", "Connection timeout %s".format(e))
                })
                .on(Socket.EVENT_CONNECT, { e ->
                    Log.d("SOCKET", "Connection established %s".format(e))
                })
        webSocket!!.connect()
    }

    fun sendEarPositions(ears: List<FloatArray>) {
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