package ua.com.radiokot.photoprism

import org.junit.Assert
import org.junit.Test
import ua.com.radiokot.photoprism.util.DbscanClustering

class DbscanClusteringTest {
    @JvmInline
    private value class Point(
        val value: Long,
    )

    @Test
    fun clusterSuccessfully() {
        val points = listOf(
            Point(1L),
            Point(2L),
            Point(3L),
            Point(8L),
            Point(9L),
            Point(25L)
        )

        val clusters = DbscanClustering(points, Point::value).cluster(
            maxDistance = 3,
            minClusterSize = 1,
        )

        Assert.assertArrayEquals(
            listOf(
                listOf(points[0], points[1], points[2]),
                listOf(points[3], points[4]),
                listOf(points[5]),
            ).toTypedArray(),
            clusters.toTypedArray()
        )
    }

    @Test
    fun clusterSuccessfully_IfHasOutliers() {
        val points = listOf(
            Point(1L),
            Point(2L),
            Point(3L),
            Point(8L),
            Point(9L),
            Point(25L)
        )

        val clusters = DbscanClustering(points, Point::value).cluster(
            maxDistance = 3,
            minClusterSize = 2,
        )

        Assert.assertArrayEquals(
            listOf(
                listOf(points[0], points[1], points[2]),
                listOf(points[3], points[4]),
            ).toTypedArray(),
            clusters.toTypedArray()
        )
    }

    @Test
    fun clusterSuccessfully_IfSingleCluster() {
        val points = listOf(
            Point(1), Point(2), Point(3),
            Point(4), Point(5)
        )
        val clusters = DbscanClustering(points, Point::value).cluster(
            maxDistance = 1,
            minClusterSize = 2,
        )
        println(clusters)
        Assert.assertEquals(1, clusters.size)
    }

    @Test
    fun clusterSuccessfully_IfMultipleClusters() {
        val points =
            listOf(
                Point(1), Point(2), Point(10), Point(11),
                Point(20), Point(21), Point(30)
            )
        val clusters = DbscanClustering(points, Point::value).cluster(
            maxDistance = 1,
            minClusterSize = 2,
        )
        Assert.assertEquals(3, clusters.size)
    }

    @Test
    fun clusterSuccessfully_IfEmptyData() {
        val clusters = DbscanClustering(emptyList(), Point::value).cluster(
            maxDistance = 1,
            minClusterSize = 2,
        )
        Assert.assertTrue(clusters.isEmpty())
    }
}
