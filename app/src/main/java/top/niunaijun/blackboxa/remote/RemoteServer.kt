package top.niunaijun.blackboxa.remote

import android.content.res.AssetManager
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import top.niunaijun.blackboxa.data.AppsRepository

/** Minimal HTTP server that lets the web panel on a PC manage spaces; every API call needs [token]. */
class RemoteServer(
        private val repo: AppsRepository,
        private val assets: AssetManager,
        private val token: String,
        private val port: Int
) {

    private class Request(val method: String, val path: String, val token: String?, val body: String)

    private class Response(val status: String, val contentType: String, val body: ByteArray)

    private val executor = Executors.newFixedThreadPool(WORKERS)
    private var serverSocket: ServerSocket? = null

    @Throws(IOException::class)
    fun start() {
        val socket = ServerSocket(port)
        serverSocket = socket
        thread(name = "RemoteServer") {
            while (!socket.isClosed) {
                try {
                    val client = socket.accept()
                    executor.execute { handle(client) }
                } catch (e: IOException) {
                    if (!socket.isClosed) Log.e(TAG, "Error accepting a connection", e)
                }
            }
        }
    }

    fun stop() {
        serverSocket?.close()
        executor.shutdownNow()
    }

    private fun handle(client: Socket) {
        client.use { socket ->
            try {
                socket.soTimeout = TIMEOUT_MS
                val request = readRequest(socket) ?: return
                val response = route(request)
                val header = "HTTP/1.1 ${response.status}\r\n" +
                        "Content-Type: ${response.contentType}\r\n" +
                        "Content-Length: ${response.body.size}\r\n" +
                        "Cache-Control: no-store\r\n" +
                        "Connection: close\r\n\r\n"
                socket.getOutputStream().apply {
                    write(header.toByteArray(StandardCharsets.ISO_8859_1))
                    write(response.body)
                    flush()
                }
            } catch (e: IOException) {
                Log.w(TAG, "Error serving a request", e)
            }
        }
    }

    private fun readRequest(socket: Socket): Request? {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1))
        val requestLine = reader.readLine()?.split(" ") ?: return null
        if (requestLine.size < 2) return null

        var contentLength = 0
        var headerToken: String? = null
        while (true) {
            val line = reader.readLine()
            if (line.isNullOrEmpty()) break
            val separator = line.indexOf(':')
            if (separator < 0) continue
            val value = line.substring(separator + 1).trim()
            when (line.substring(0, separator).lowercase()) {
                "content-length" -> contentLength = value.toIntOrNull() ?: return null
                "x-token" -> headerToken = value
            }
        }
        if (contentLength !in 0..MAX_BODY_BYTES) return null

        val raw = CharArray(contentLength)
        var read = 0
        while (read < contentLength) {
            val count = reader.read(raw, read, contentLength - read)
            if (count < 0) return null
            read += count
        }
        val body = String(String(raw).toByteArray(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)

        val target = requestLine[1]
        val path = target.substringBefore('?')
        val queryToken = target.substringAfter('?', "").split('&')
                .firstOrNull { it.startsWith("t=") }?.substring(2)
        return Request(requestLine[0], path, headerToken ?: queryToken, body)
    }

    private fun route(request: Request): Response {
        if (request.path == "/") {
            return Response(OK, "text/html; charset=utf-8", assets.open(PANEL_ASSET).use { it.readBytes() })
        }
        if (!isAuthorized(request.token)) return error(UNAUTHORIZED, "Unauthorized")
        if (request.path != "/api/users") return error(NOT_FOUND, "Not found")
        return when (request.method) {
            "GET" -> listUsers()
            "POST" -> createUser(request.body)
            else -> error(METHOD_NOT_ALLOWED, "Method not allowed")
        }
    }

    private fun isAuthorized(candidate: String?): Boolean =
            candidate != null &&
                    MessageDigest.isEqual(
                            candidate.toByteArray(StandardCharsets.UTF_8),
                            token.toByteArray(StandardCharsets.UTF_8)
                    )

    private fun listUsers(): Response {
        val users = runBlocking { repo.getUserList() }
        val array = JSONArray()
        users.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("appCount", it.appCount)) }
        return json(OK, array.toString())
    }

    private fun createUser(body: String): Response {
        val name = try {
            JSONObject(body).optString("name").trim()
        } catch (e: JSONException) {
            return error(BAD_REQUEST, "Invalid JSON")
        }
        if (name.length > MAX_NAME_LENGTH) return error(BAD_REQUEST, "Name too long")

        val result = runBlocking { repo.createUser(name, installGms = false) }
        val user = result.user ?: return error(INTERNAL_ERROR, result.error.orEmpty())
        return json(CREATED, JSONObject().put("id", user.id).put("name", user.name).toString())
    }

    private fun error(status: String, message: String) =
            json(status, JSONObject().put("error", message).toString())

    private fun json(status: String, body: String) =
            Response(status, "application/json; charset=utf-8", body.toByteArray(StandardCharsets.UTF_8))

    private companion object {
        const val TAG = "RemoteServer"
        const val PANEL_ASSET = "remote/index.html"
        const val WORKERS = 4
        const val TIMEOUT_MS = 5000
        const val MAX_BODY_BYTES = 4096
        const val MAX_NAME_LENGTH = 40
        const val OK = "200 OK"
        const val CREATED = "201 Created"
        const val BAD_REQUEST = "400 Bad Request"
        const val UNAUTHORIZED = "401 Unauthorized"
        const val NOT_FOUND = "404 Not Found"
        const val METHOD_NOT_ALLOWED = "405 Method Not Allowed"
        const val INTERNAL_ERROR = "500 Internal Server Error"
    }
}
