package com.solidkey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.solidkey.demo.AgentPatchBus
import com.solidkey.demo.DemoApp
import java.net.InetAddress
import java.net.ServerSocket

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // PoC transport for the live-agent-controlled game: a tiny loopback socket. An external agent
        // sends one JSON patch per line, reached over adb:
        //   adb forward tcp:8787 tcp:8787
        //   printf '%s\n' '{"op":"wave","count":6}' | nc -w1 127.0.0.1 8787
        // each line is forwarded to AgentPatchBus for the running game to apply live. Stands in for
        // the nexum / Firebase realtime channel in the full design (LIVE_AGENT_GAMES_SPEC.md).
        startPatchServer()

        setContent {
            DemoApp()
        }
    }

    private fun startPatchServer() {
        Thread {
            try {
                val server = ServerSocket(PATCH_PORT, 50, InetAddress.getByName("127.0.0.1"))
                android.util.Log.i("AGENTPOC", "patch server listening on 127.0.0.1:$PATCH_PORT")
                while (true) {
                    val sock = server.accept()
                    try {
                        sock.getInputStream().bufferedReader().forEachLine { line ->
                            val t = line.trim()
                            if (t.isNotEmpty()) AgentPatchBus.emit(t)
                        }
                    } catch (_: Exception) {
                    } finally {
                        runCatching { sock.close() }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AGENTPOC", "patch server failed", e)
            }
        }.apply { isDaemon = true }.start()
    }

    private companion object {
        const val PATCH_PORT = 8787
    }
}
