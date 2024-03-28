package ua.com.radiokot.photoprism.util

import kotlin.math.abs


/**
 * DBSCAN clustering algorithm for one-dimensional data.
 *
 * @see <a href="https://github.com/chrfrantz/DBSCAN/">Reference Java implementation</a>
 */
class DbscanClustering<Item : Any>(
    private val items: List<Item>,
    private val keySelector: (Item) -> Long,
) {
    /**
     * @param maxDistance maximum distance between 2 items for them to be considered neighbors
     * @param minClusterSize minimum number of neighbors an item needs to have to be considered a core point, including the starting point
     */
    fun cluster(
        maxDistance: Long,
        minClusterSize: Int,
    ): List<List<Item>> {
        val clusters = mutableListOf<List<Item>>()
        val visitedItems = BooleanArray(items.size)

        items.forEachIndexed corePointsSearch@{ corePointIndex, corePoint ->
            if (visitedItems[corePointIndex]) {
                return@corePointsSearch
            }
            visitedItems[corePointIndex] = true

            val neighborIndices: MutableList<Int> = getNeighborIndices(corePoint, maxDistance)
            if (neighborIndices.size >= minClusterSize) {
                var neighborIndexIndex = 0
                while (neighborIndexIndex < neighborIndices.size) {
                    val neighborIndex = neighborIndices[neighborIndexIndex]
                    if (visitedItems[neighborIndex]) {
                        neighborIndexIndex++
                        continue
                    }
                    visitedItems[neighborIndex] = true

                    val neighbor = items[neighborIndex]
                    val extraNeighborIndices = getNeighborIndices(neighbor, maxDistance)

                    if (extraNeighborIndices.size >= minClusterSize) {
                        neighborIndices.addAll(extraNeighborIndices)
                    }

                    neighborIndexIndex++
                }

                clusters.add(neighborIndices.distinct().map(items::get))
            }
        }

        return clusters
    }

    private fun getNeighborIndices(
        item: Item,
        maxDistance: Long,
    ): MutableList<Int> =
        items.mapIndexedNotNull { neighborCandidateIndex, neighborCandidate ->
            if (neighborCandidate.isNeighborOf(item, maxDistance)) {
                neighborCandidateIndex
            } else {
                null
            }
        }.toMutableList()

    private fun Item.isNeighborOf(
        other: Item,
        maxDistance: Long,
    ): Boolean =
        abs(keySelector(this) - keySelector(other)) <= maxDistance
}
