package com.solidkey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.solidkey.demo.AgentPatchBus
import com.solidkey.demo.AgentStateBus
import com.solidkey.demo.DemoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Production transport for the live-agent-controlled game (see
 * KMPMedia-internal-docs/LIVE_AGENT_GAMES_SPEC.md). Replaces the PoC loopback socket (adb + nc) with
 * a REAL realtime wire: **nexum's own Firebase Realtime Database** (project loom-45438). The library
 * stays network-free — all I/O lives here in the app.
 *
 * The app:
 *   1. signs in **anonymously** (nexum's RTDB rules allow an authed client under `games/` only),
 *   2. **streams** agent patches from `games/{ws}/{session}/patches` over Server-Sent Events and
 *      pushes each onto [AgentPatchBus] for the running UFO Dodge loop to apply live, and
 *   3. **publishes** the game's 1 Hz state snapshot to `games/{ws}/{session}/state`, so the nexum
 *      agent can observe what it is steering.
 *
 * The nexum agent (the "director") writes patches to the same path. Transport sits entirely behind
 * [AgentPatchBus] / [AgentStateBus], so the game logic is unchanged from the socket PoC.
 *
 * The Web API key below is not a secret (it ships in every Firebase client); security is enforced by
 * the RTDB rules + anonymous auth, not by hiding the key.
 */
class MainActivity : ComponentActivity() {

    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var idToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startRtdbTransport()
        setContent {
            DemoApp()
        }
    }

    private fun startRtdbTransport() {
        io.launch {
            if (signInAnonymously() == null) {
                android.util.Log.e(TAG, "no anon token — agent transport disabled")
                return@launch
            }
            // State publisher: push every 1 Hz snapshot the game emits onto AgentStateBus.
            io.launch {
                AgentStateBus.states.collect { json ->
                    idToken?.let { t ->
                        runCatching { request("$BASE/state.json?auth=$t", "PUT", json) }
                    }
                }
            }
            // Patch stream: read SSE until it drops, then re-auth + reconnect.
            while (true) {
                runCatching { streamPatches() }
                    .onFailure { android.util.Log.w(TAG, "patch stream dropped — reconnecting", it) }
                Thread.sleep(1500)
                signInAnonymously()   // refresh the id token before reconnecting
            }
        }
    }

    /** Anonymous Firebase sign-in → a short-lived id token. Stored in [idToken]. */
    private fun signInAnonymously(): String? = runCatching {
        val resp = request(
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$API_KEY",
            "POST",
            """{"returnSecureToken":true}""",
        )
        JSONObject(resp).getString("idToken").also {
            idToken = it
            android.util.Log.i(TAG, "anon sign-in ok")
        }
    }.onFailure { android.util.Log.e(TAG, "anon sign-in failed", it) }.getOrNull()

    /** Open the RTDB SSE stream on the patches node and dispatch each event. Blocks until it ends. */
    private fun streamPatches() {
        val token = idToken ?: return
        val url = URL("$BASE/patches.json?auth=$token")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "text/event-stream")
            connectTimeout = 15_000
            readTimeout = 0   // stream indefinitely; RTDB sends periodic keep-alives
        }
        android.util.Log.i(TAG, "patch stream open: games/$WS/$SESSION/patches")
        conn.inputStream.bufferedReader().use { br ->
            var event: String? = null
            val data = StringBuilder()
            br.forEachLine { line ->
                when {
                    line.startsWith("event:") -> event = line.substring(6).trim()
                    line.startsWith("data:") -> data.append(line.substring(5).trim())
                    line.isEmpty() -> {
                        if (event == "put") handlePut(data.toString())
                        event = null
                        data.setLength(0)
                    }
                }
            }
        }
    }

    /**
     * Handle one RTDB `put` SSE payload `{"path":..,"data":..}`. A fresh child under `patches` is a
     * new agent patch; drain it onto the bus and delete it so it is applied exactly once and the
     * queue stays small. The initial `path:"/"` snapshot drains any patches that arrived before
     * the app connected.
     */
    private fun handlePut(dataStr: String) {
        if (dataStr.isEmpty() || dataStr == "null") return
        val obj = runCatching { JSONObject(dataStr) }.getOrNull() ?: return
        val path = obj.optString("path", "/")
        val payload = obj.opt("data")
        when {
            path == "/" && payload is JSONObject -> {
                val keys = payload.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    (payload.opt(id) as? JSONObject)?.let { consume(id, it) }
                }
            }
            path.length > 1 && payload is JSONObject -> consume(path.substring(1), payload)
            // null payload = a deletion we made, or an empty node → ignore
        }
    }

    private fun consume(childId: String, patch: JSONObject) {
        val json = patch.toString()
        AgentPatchBus.emit(json)
        android.util.Log.i(TAG, "patch applied: $json")
        idToken?.let { t ->
            runCatching { request("$BASE/patches/$childId.json?auth=$t", "DELETE", null) }
        }
    }

    /** Minimal HTTP helper (no deps). Returns the response body, or throws on a transport error. */
    private fun request(urlStr: String, method: String, body: String?): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        if (body != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) android.util.Log.w(TAG, "$method $urlStr -> $code $text")
        return text
    }

    private companion object {
        const val TAG = "AGENTPOC"
        // nexum's own Firebase RTDB (project loom-45438, europe-west1). Not a secret.
        const val API_KEY = "AIzaSyDIM7iD8TvXUum966kcn__3-BvsQ25bB-Y"
        const val DB = "https://loom-45438-default-rtdb.europe-west1.firebasedatabase.app"
        const val WS = "kmpmedia"
        const val SESSION = "poc"
        const val BASE = "$DB/games/$WS/$SESSION"
    }
}
