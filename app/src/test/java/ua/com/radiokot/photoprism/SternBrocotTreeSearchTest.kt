package ua.com.radiokot.photoprism

import org.junit.Assert
import org.junit.Test
import ua.com.radiokot.photoprism.util.SternBrocotTreeSearch

class SternBrocotTreeSearchTest {
    @Test
    fun initialState() {
        val t = SternBrocotTreeSearch()
        Assert.assertEquals(1, t.numerator)
        Assert.assertEquals(1, t.denominator)
        Assert.assertEquals(1.0, t.value, 0.0)
        Assert.assertEquals(0, t.depth)
    }

    @Test
    fun paths() {
        SternBrocotTreeSearch()
            .goLeft()
            .goLeft()
            .goLeft()
            .apply {
                Assert.assertEquals(1, numerator)
                Assert.assertEquals(4, denominator)
            }

        SternBrocotTreeSearch()
            .goLeft()
            .goLeft()
            .goRight()
            .apply {
                Assert.assertEquals(2, numerator)
                Assert.assertEquals(5, denominator)
            }

        SternBrocotTreeSearch()
            .goLeft()
            .goRight()
            .goLeft()
            .apply {
                Assert.assertEquals(3, numerator)
                Assert.assertEquals(5, denominator)
            }

        SternBrocotTreeSearch()
            .goLeft()
            .goRight()
            .goRight()
            .apply {
                Assert.assertEquals(3, numerator)
                Assert.assertEquals(4, denominator)
            }
    }

    @Test
    fun goToDouble() {
        SternBrocotTreeSearch()
            .goTo(Math.PI)
            .apply {
                Assert.assertEquals(Math.PI, value, 0.0)
                Assert.assertEquals(245850922, numerator)
                Assert.assertEquals(78256779, denominator)
                Assert.assertEquals(344, depth)
            }
    }

    @Test
    fun goToRational() {
        SternBrocotTreeSearch()
            .goTo(15, 16)
            .apply {
                Assert.assertEquals(15, numerator)
                Assert.assertEquals(16, denominator)
                Assert.assertEquals(0.9375, value, 0.0)
                Assert.assertEquals(15, depth)
            }
    }
}