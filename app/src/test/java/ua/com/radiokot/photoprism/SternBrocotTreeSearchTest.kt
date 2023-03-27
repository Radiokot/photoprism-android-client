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
    fun goBetween() {
        SternBrocotTreeSearch()
            .goBetween(15.0 / 16, 1.0)
            .apply {
                Assert.assertEquals(16, numerator)
                Assert.assertEquals(17, denominator)
            }

        SternBrocotTreeSearch()
            .goBetween(44320.0 / 39365, 77200.0 / 12184)
            .apply {
                Assert.assertEquals(2, numerator)
                Assert.assertEquals(1, denominator)
            }

        SternBrocotTreeSearch()
            .goBetween(1.0 / 3, Double.POSITIVE_INFINITY)
            .apply {
                Assert.assertEquals(1, numerator)
                Assert.assertEquals(1, denominator)
            }

        SternBrocotTreeSearch()
            .goBetween(3.0 / 2, Double.POSITIVE_INFINITY)
            .apply {
                Assert.assertEquals(2, numerator)
                Assert.assertEquals(1, denominator)
            }

        SternBrocotTreeSearch()
            .goBetween(0.0, 15.0/16)
            .apply {
                Assert.assertEquals(1, numerator)
                Assert.assertEquals(2, denominator)
            }

        SternBrocotTreeSearch()
            .goBetween(0.0, Double.POSITIVE_INFINITY)
            .apply {
                Assert.assertEquals(1, numerator)
                Assert.assertEquals(1, denominator)
            }
    }
}