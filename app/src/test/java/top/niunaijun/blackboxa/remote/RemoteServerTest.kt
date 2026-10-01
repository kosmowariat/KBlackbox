package top.niunaijun.blackboxa.remote

import java.net.InetAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteServerTest {

    private class FakeSpaces : SpaceDirectory {
        val created = mutableListOf<String>()

        override suspend fun list() = listOf(SpaceSummary(0, "User 0", 2))

        override suspend fun create(name: String): SpaceCreation {
            created += name
            return SpaceCreation(SpaceSummary(1, name.ifEmpty { "User 1" }, 0), null)
        }
    }

    private val spaces = FakeSpaces()
    private lateinit var server: RemoteServer

    private class Reply(val status: Int, val headers: String, val body: String)

    @Before
    fun startServer() {
        server = RemoteServer(spaces, { "<html>panel</html>".toByteArray() }, TOKEN, 0, InetAddress.getLoopbackAddress())
        server.start()
    }

    @After
    fun stopServer() = server.stop()

    private fun request(method: String, path: String, token: String? = null, body: String = ""): Reply {
        Socket(InetAddress.getLoopbackAddress(), server.localPort).use { socket ->
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            val tokenHeader = if (token != null) "X-Token: $token\r\n" else ""
            socket.getOutputStream().write(
                    ("$method $path HTTP/1.1\r\nHost: x\r\n$tokenHeader" +
                            "Content-Length: ${bytes.size}\r\n\r\n").toByteArray() + bytes
            )
            val text = socket.getInputStream().readBytes().toString(StandardCharsets.UTF_8)
            val headers = text.substringBefore("\r\n\r\n")
            return Reply(headers.lines().first().split(" ")[1].toInt(), headers, text.substringAfter("\r\n\r\n"))
        }
    }

    @Test
    fun servesThePageWithoutATokenAndWithSecurityHeaders() {
        val reply = request("GET", "/")
        assertEquals(200, reply.status)
        assertEquals("<html>panel</html>", reply.body)
        assertTrue(reply.headers.contains("X-Content-Type-Options: nosniff"))
        assertTrue(reply.headers.contains("Content-Security-Policy:"))
        assertTrue(reply.headers.contains("X-Frame-Options: DENY"))
    }

    @Test
    fun rejectsApiCallsWithoutOrWithAWrongToken() {
        assertEquals(401, request("GET", "/api/users").status)
        assertEquals(401, request("GET", "/api/users", "wrong").status)
    }

    @Test
    fun doesNotAcceptTheTokenInTheAddress() {
        assertEquals(401, request("GET", "/api/users?t=$TOKEN").status)
    }

    @Test
    fun listsSpacesForTheRightToken() {
        val reply = request("GET", "/api/users", TOKEN)
        assertEquals(200, reply.status)
        val users = JSONArray(reply.body)
        assertEquals("User 0", users.getJSONObject(0).getString("name"))
        assertEquals(2, users.getJSONObject(0).getInt("appCount"))
    }

    @Test
    fun createsASpace() {
        val reply = request("POST", "/api/users", TOKEN, """{"name":" Work "}""")
        assertEquals(201, reply.status)
        assertEquals("Work", JSONObject(reply.body).getString("name"))
        assertEquals(listOf("Work"), spaces.created)
    }

    @Test
    fun rejectsBadBodies() {
        assertEquals(400, request("POST", "/api/users", TOKEN, "not json").status)
        assertEquals(400, request("POST", "/api/users", TOKEN, """{"name":"${"x".repeat(41)}"}""").status)
        assertTrue(spaces.created.isEmpty())
    }

    @Test
    fun rejectsUnknownPathsAndMethods() {
        assertEquals(404, request("GET", "/api/other", TOKEN).status)
        assertEquals(405, request("DELETE", "/api/users", TOKEN).status)
    }

    @Test
    fun locksTheApiAfterTooManyWrongTokens() {
        repeat(RemoteServer.MAX_FAILED_ATTEMPTS) { assertEquals(401, request("GET", "/api/users", "wrong").status) }
        assertEquals(429, request("GET", "/api/users", TOKEN).status)
        assertEquals(200, request("GET", "/").status)
    }

    @Test
    fun dropsRequestsWithAnOversizedHeaderLine() {
        Socket(InetAddress.getLoopbackAddress(), server.localPort).use { socket ->
            socket.getOutputStream().write(("GET / HTTP/1.1\r\nX-Big: " + "a".repeat(5000) + "\r\n\r\n").toByteArray())
            assertTrue(socket.getInputStream().readBytes().isEmpty())
        }
    }

    private companion object {
        const val TOKEN = "abcdefghijk23456"
    }
}
