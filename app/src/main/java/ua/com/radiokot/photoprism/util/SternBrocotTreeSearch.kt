package ua.com.radiokot.photoprism.util

/**
 * A binary search for Stern–Brocot tree.
 * Right direction is for bigger numbers, left – for smaller.
 *
 * @see goTo
 * @see <a href="https://en.wikipedia.org/wiki/Stern%E2%80%93Brocot_tree">Stern–Brocot tree</a>
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
     * Goes to the target rational, so the [numerator] and [denominator] will be set at the end.
     */
    fun goTo(targetN: Int, targetD: Int): SternBrocotTreeSearch =
        goTo(targetN.toDouble() / targetD)

    /**
     * Goes to the [target], so the [value] will set at the end.
     */
    fun goTo(target: Double) = apply {
        while (value != target) {
            if (target > value) {
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