package top.niunaijun.blackboxa.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MathUtilTest {

    @Test
    fun distanceBetweenTwoPoints() {
        assertEquals(5, MathUtil.getDistance(0f, 0f, 3f, 4f))
    }

    @Test
    fun distanceToTheSamePointIsZero() {
        assertEquals(0, MathUtil.getDistance(2f, 2f, 2f, 2f))
    }

    @Test
    fun convertsDegreesToRadians() {
        assertEquals(Math.PI, MathUtil.angle2Radian(180.0), 1e-9)
        assertEquals(Math.PI / 2, MathUtil.angle2Radian(90.0), 1e-9)
    }
}
