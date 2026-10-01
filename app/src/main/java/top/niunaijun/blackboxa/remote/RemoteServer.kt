package top.niunaijun.blackboxa.remote

import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
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

/**
 * Minimal HTTP server that lets the web panel on a PC manage spaces. The page itself is public;
 * every API call needs the token in the X-Token header, and too many wrong tokens lock the API for a while.
 */
class RemoteServer(
        private val spaces: SpaceDirectory,
        private val panelPage: () -> ByteArray,
        private val token: String,
        private val port: Int,
        private val bindAddress: InetAddress? = null,
        private val guard: AuthGuard = AuthGuard(MAX_FAILED_ATTEMPTS, LOCK_MS)
) {

    private class Request(val method: String, val path: String, val token: String?, val body: String)

    private class Response(val status: String, val contentType: String, val body: ByteArray)

    private val executor = Executors.newFixedThreadPool(WORKERS)
    private var serverSocket: ServerSocket? = null

    val localPort: Int
        get() = serverSocket?.localPort ?: -1

    @Throws(IOException::class)
    fun start() {
        val socket = ServerSocket(port, BACKLOG, bindAddress)
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
                        "X-Content-Type-Options: nosniff\r\n" +
                        "X-Frame-Options: DENY\r\n" +
                        "Referrer-Policy: no-referrer\r\n" +
                        "Content-Security-Policy: $CONTENT_SECURITY_POLICY\r\n" +
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

    /** Reads one line, giving up on lines that are too long or requests that take too long overall. */
    private fun readLine(reader: BufferedReader, deadline: Long): String? {
        val line = StringBuilder()
        while (true) {
            if (System.currentTimeMillis() > deadline) return null
            val c = reader.read()
            if (c < 0) return if (line.isEmpty()) null else line.toString()
            if (c == '\n'.code) return line.toString().trimEnd('\r')
            line.append(c.toChar())
            if (line.length > MAX_LINE_CHARS) return null
        }
    }

    private fun readRequest(socket: Socket): Request? {
        val deadline = System.currentTimeMillis() + REQUEST_DEADLINE_MS
        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1))
        val requestLine = readLine(reader, deadline)?.split(" ") ?: return null
        if (requestLine.size < 2) return null

        var contentLength = 0
        var headerToken: String? = null
        var headerCount = 0
        while (true) {
            val line = readLine(reader, deadline) ?: return null
            if (line.isEmpty()) break
            if (++headerCount > MAX_HEADERS) return null
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
            if (System.currentTimeMillis() > deadline) return null
            val count = reader.read(raw, read, contentLength - read)
            if (count < 0) return null
            read += count
        }
        val body = String(String(raw).toByteArray(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)
        return Request(requestLine[0], requestLine[1].substringBefore('?'), headerToken, body)
    }

    private fun route(request: Request): Response {
        if (request.path == "/") {
            return Response(OK, "text/html; charset=utf-8", panelPage())
        }
        if (guard.isLocked()) return error(TOO_MANY_REQUESTS, "Too many wrong tokens, try again later")
        if (!isAuthorized(request.token)) {
            guard.recordFailure()
            return error(UNAUTHORIZED, "Unauthorized")
        }
        guard.recordSuccess()
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
        val array = JSONArray()
        runBlocking { spaces.list() }.forEach {
            array.put(JSONObject().put("id", it.id).put("name", it.name).put("appCount", it.appCount))
        }
        return json(OK, array.toString())
    }

    private fun createUser(body: String): Response {
        val name = try {
            JSONObject(body).optString("name").trim()
        } catch (e: JSONException) {
            return error(BAD_REQUEST, "Invalid JSON")
        }
        if (name.length > MAX_NAME_LENGTH) return error(BAD_REQUEST, "Name too long")

        val created = runBlocking { spaces.create(name) }
        val space = created.space ?: return error(INTERNAL_ERROR, created.error.orEmpty())
        return json(CREATED, JSONObject().put("id", space.id).put("name", space.name).toString())
    }

    private fun error(status: String, message: String) =
            json(status, JSONObject().put("error", message).toString())

    private fun json(status: String, body: String) =
            Response(status, "application/json; charset=utf-8", body.toByteArray(StandardCharsets.UTF_8))

    companion object {
        private const val TAG = "RemoteServer"
        const val MAX_FAILED_ATTEMPTS = 10
        const val LOCK_MS = 60_000L
        private const val WORKERS = 4
        private const val BACKLOG = 16
        private const val TIMEOUT_MS = 5000
        private const val REQUEST_DEADLINE_MS = 10_000L
        private const val MAX_LINE_CHARS = 2048
        private const val MAX_HEADERS = 32
        private const val MAX_BODY_BYTES = 4096
        private const val MAX_NAME_LENGTH = 40
        private const val CONTENT_SECURITY_POLICY =
                "default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; " +
                        "connect-src 'self'; frame-ancestors 'none'"
        private const val OK = "200 OK"
        private const val CREATED = "201 Created"
        private const val BAD_REQUEST = "400 Bad Request"
        private const val UNAUTHORIZED = "401 Unauthorized"
        private const val NOT_FOUND = "404 Not Found"
        private const val METHOD_NOT_ALLOWED = "405 Method Not Allowed"
        private const val TOO_MANY_REQUESTS = "429 Too Many Requests"
        private const val INTERNAL_ERROR = "500 Internal Server Error"
    }
}
