package top.niunaijun.blackboxa.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoCoordinatesTest {

    @Test
    fun parsesCommaSeparatedPair() {
        assertEquals(52.2297 to 21.0122, GeoCoordinates.parse("52.2297, 21.0122"))
    }

    @Test
    fun parsesSpaceAndSemicolonSeparatedPairs() {
        assertEquals(-33.86 to 151.21, GeoCoordinates.parse("-33.86 151.21"))
        assertEquals(10.0 to 20.0, GeoCoordinates.parse("10;20"))
    }

    @Test
    fun rejectsOutOfRangeValues() {
        assertNull(GeoCoordinates.parse("91, 10"))
        assertNull(GeoCoordinates.parse("10, 181"))
    }

    @Test
    fun rejectsMalformedInput() {
        assertNull(GeoCoordinates.parse(""))
        assertNull(GeoCoordinates.parse("52.2297"))
        assertNull(GeoCoordinates.parse("a, b"))
        assertNull(GeoCoordinates.parse("1, 2, 3"))
    }
}
