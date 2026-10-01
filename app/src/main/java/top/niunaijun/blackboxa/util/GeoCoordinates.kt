package top.niunaijun.blackboxa.util

/** Parses "latitude, longitude" typed by the user; returns null when it is not a valid position. */
object GeoCoordinates {

    private const val MAX_LATITUDE = 90.0
    private const val MAX_LONGITUDE = 180.0
    private val SEPARATOR = Regex("[,;\\s]+")

    fun parse(text: String): Pair<Double, Double>? {
        val parts = text.trim().split(SEPARATOR).filter { it.isNotEmpty() }
        if (parts.size != 2) {
            return null
        }
        val latitude = parts[0].toDoubleOrNull() ?: return null
        val longitude = parts[1].toDoubleOrNull() ?: return null
        if (latitude !in -MAX_LATITUDE..MAX_LATITUDE || longitude !in -MAX_LONGITUDE..MAX_LONGITUDE) {
            return null
        }
        return latitude to longitude
    }
}
