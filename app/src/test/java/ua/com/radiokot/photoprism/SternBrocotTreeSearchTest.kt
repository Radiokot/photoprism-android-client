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
            .goBetween(0.0, 15.0 / 16)
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

        SternBrocotTreeSearch()
            .goBetween(0.001, 0.0011)
            .apply {
                Assert.assertEquals(1, numerator)
                Assert.assertEquals(910, denominator)
            }

        SternBrocotTreeSearch()
            .goBetween(99.99, 100.01)
            .apply {
                Assert.assertEquals(100, numerator)
                Assert.assertEquals(1, denominator)
            }

        SternBrocotTreeSearch()
            .goBetween(1.0 / 3, 0.333334, maxDepth = 20000)
            .apply {
                Assert.assertEquals(20000, depth)
            }
    }

    @Test
    fun extensiveTest() {
        data class Item(
            val name: String,
            val position: Double,
        )

        val originalList: List<Item> = buildList {
            val sbt = SternBrocotTreeSearch()
            repeat(100) {
                add(
                    Item(
                        name = "Item #${it.toString().padStart(3, '0')}",
                        position = sbt.goRight().value,
                    )
                )
            }
        }
        val shuffleList = originalList.toMutableList()
        val sbtList = originalList.toMutableList()

        // The double-based SBT implementation allows around 1000-7000 movements
        // before positions get messed up.
        repeat(50000) { i ->
            var itemToMoveIndex: Int
            var itemToInsertBeforeIndex: Int
            do {
                itemToMoveIndex = originalList.indices.random()
                itemToInsertBeforeIndex = originalList.indices.random()
            } while (itemToMoveIndex == itemToInsertBeforeIndex)

            val itemToMoveName = shuffleList[itemToMoveIndex].name
            val itemToInsertBeforeName = shuffleList[itemToInsertBeforeIndex].name

            println("$i Place $itemToMoveName before $itemToInsertBeforeName")

            // Straightforward.
            shuffleList.removeAll { it.name == itemToMoveName }
            shuffleList.add(
                shuffleList.indexOfFirst { it.name == itemToInsertBeforeName },
                originalList.first { it.name == itemToMoveName },
            )

            // SBT.
            if (itemToMoveIndex == itemToInsertBeforeIndex - 2
                || itemToMoveIndex == itemToInsertBeforeIndex + 1
            ) {
                // Special case for swap.
                val swapAIndex = itemToMoveIndex
                val swapA = sbtList[swapAIndex]
                val swapBIndex =
                    if (itemToInsertBeforeIndex > itemToMoveIndex)
                        itemToMoveIndex + 1
                    else
                        itemToMoveIndex - 1
                val swapB = sbtList[swapBIndex]
                sbtList[swapAIndex] = swapB.copy(
                    position = swapA.position
                )
                sbtList[swapBIndex] = swapA.copy(
                    position = swapB.position
                )
            } else {
                val sbtItemToMove = sbtList[itemToMoveIndex]
                val sbtItemToInsertBefore = sbtList[itemToInsertBeforeIndex]

                val prevToDestinationPosition = sbtList
                    .filter { it.position < sbtItemToInsertBefore.position }
                    .maxOfOrNull(Item::position)
                    ?: 0.0
                val targetPosition = SternBrocotTreeSearch()
                    .goBetween(
                        lowerBound = prevToDestinationPosition,
                        upperBound = sbtItemToInsertBefore.position,
                    )
                    .apply {
                        Assert.assertTrue("SBT numerator must not overflow", numerator >= 0)
                        Assert.assertTrue("SBT denominator must not overflow", denominator > 0)
                    }
                    .value
                sbtList[itemToMoveIndex] =
                    sbtItemToMove.copy(
                        position = targetPosition
                    )
            }

            sbtList.sortBy(Item::position)

            Assert.assertEquals(
                shuffleList.map(Item::name),
                sbtList.map(Item::name)
            )
        }
    }
}
