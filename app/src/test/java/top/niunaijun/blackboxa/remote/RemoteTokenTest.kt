package top.niunaijun.blackboxa.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteTokenTest {

    @Test
    fun hasTheExpectedLengthAndOnlyAllowedCharacters() {
        val token = RemoteToken.generate()
        assertEquals(RemoteToken.LENGTH, token.length)
        assertTrue(token.all { it in RemoteToken.ALPHABET })
    }

    @Test
    fun tokensDiffer() {
        assertNotEquals(RemoteToken.generate(), RemoteToken.generate())
    }
}
