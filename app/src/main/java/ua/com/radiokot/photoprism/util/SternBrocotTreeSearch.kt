package ua.com.radiokot.photoprism.util

/**
 * A binary search for Stern–Brocot tree.
 * Right direction is for bigger numbers, left – for smaller.
 *
 * @see goBetween
 *
 * @see <a href="https://en.wikipedia.org/wiki/Stern%E2%80%93Brocot_tree">Wikipedia</a>
 * @see <a href="https://begriffs.com/posts/2018-03-20-user-defined-order.html">Use case</a>
 */
class SternBrocotTreeSearch {
    /**
     * Current fraction numerator.
     */
    var numerator = 1

    /**
     * Current fraction denominator.
     */
    var denominator = 1

    private var closestRightN = 1
    private var closestRightD = 0

    private var closestLeftN = 0
    private var closestLeftD = 1

    /**
     * Current fraction decimal value.
     */
    val value: Double
        get() = numerator.toDouble() / denominator

    /**
     * Current node depth, starting from 0.
     */
    var depth: Int = 0
        private set

    /**
     * Goes deeper in the right direction (for bigger numbers).
     */
    fun goRight() = apply {
        val newN = numerator + closestRightN
        val newD = denominator + closestRightD

        closestLeftN = numerator
        closestLeftD = denominator

        numerator = newN
        denominator = newD

        depth++
    }

    /**
     * Goes deeper in the left direction (for small numbers).
     */
    fun goLeft() = apply {
        val newN = numerator + closestLeftN
        val newD = denominator + closestLeftD

        closestRightN = numerator
        closestRightD = denominator

        numerator = newN
        denominator = newD

        depth++
    }

    /**
     * Goes to the fraction laying within the given bounds (exclusively).
     * Minimal lower bound is 0.0
     * Maximal upper bound is [Double.POSITIVE_INFINITY]
     */
    fun goBetween(lowerBound: Double, upperBound: Double) = apply {
        require(lowerBound < upperBound) {
            "Lower bound must be smaller than the upper one"
        }

        require(lowerBound >= 0.0) {
            "Lower bound can't be smaller than 0"
        }

        while (!(value > lowerBound && value < upperBound)) {
            if (value <= lowerBound && value <= upperBound) {
                goRight()
            } else {
                goLeft()
            }
        }
    }

    override fun toString(): String {
        return "SternBrocotTreeSearch($numerator/$denominator, $value, depth=$depth)"
    }
}