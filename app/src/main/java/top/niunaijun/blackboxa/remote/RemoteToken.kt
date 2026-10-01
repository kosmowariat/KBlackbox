package top.niunaijun.blackboxa.remote

import java.security.SecureRandom

object RemoteToken {

    const val LENGTH = 16
    const val ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789"

    fun generate(random: SecureRandom = SecureRandom()): String =
            String(CharArray(LENGTH) { ALPHABET[random.nextInt(ALPHABET.length)] })
}
