package ua.com.radiokot.photoprism.features.viewer.view.model

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The GOAT structure for storing orientation.
 * Thread safe.
 */
class Quaternion(
    private var w: Float = 1f,
    private var x: Float = 0f,
    private var y: Float = 0f,
    private var z: Float = 0f,
) {
    fun set(
        w: Float,
        x: Float,
        y: Float,
        z: Float,
    ) = synchronized(this) {
        this.w = w
        this.x = x
        this.y = y
        this.z = z
    }

    fun set(
        other: Quaternion,
    ) = set(
        w = other.w,
        x = other.x,
        y = other.y,
        z = other.z,
    )

    fun setIdentity() =
        set(
            w = 1f,
            x = 0f,
            y = 0f,
            z = 0f,
        )

    /**
     * @param deltaPitch in radians
     * @param deltaYaw in radians
     * @param deltaRoll in radians
     */
    fun rotateAroundItself(
        deltaPitch: Float,
        deltaYaw: Float,
        deltaRoll: Float,
    ) = synchronized(this) {
        val angle = sqrt(deltaPitch * deltaPitch + deltaYaw * deltaYaw + deltaRoll * deltaRoll)

        if (angle < 0.0000001f) {
            return@synchronized
        }

        val half = angle * 0.5f
        val k = sin(half) / angle
        multiplyRight(cos(half), deltaPitch * k, deltaYaw * k, deltaRoll * k)

        normalize()
    }

    /**
     * @param angle in radians
     */
    fun rotateAroundAxis(
        angle: Float,
        axisX: Float,
        axisY: Float,
        axisZ: Float,
    ) = synchronized(this) {
        val len = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)

        if (len < 0.000001 || abs(angle) < 0.0000001) {
            return@synchronized
        }

        val half = angle * 0.5f
        val k = sin(half) / len
        multiplyLeft(cos(half), axisX * k, axisY * k, axisZ * k)

        normalize()
    }

    /**
     * @param out 4x4 result matrix, float array of 16 elements.
     */
    fun getViewMatrix(out: FloatArray) = synchronized(this) {
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yz = y * z
        val wx = w * x
        val wy = w * y
        val wz = w * z

        out[0] = 1f - 2f * (yy + zz)
        out[1] = 2f * (xy - wz)
        out[2] = 2f * (xz + wy)
        out[3] = 0f

        out[4] = 2f * (xy + wz)
        out[5] = 1f - 2f * (xx + zz)
        out[6] = 2f * (yz - wx)
        out[7] = 0f

        out[8] = 2f * (xz - wy)
        out[9] = 2f * (yz + wx)
        out[10] = 1f - 2f * (xx + yy)
        out[11] = 0f

        out[12] = 0f
        out[13] = 0f
        out[14] = 0f
        out[15] = 1f
    }

    private fun normalize() {
        val n = sqrt(w * w + x * x + y * y + z * z)

        if (n < 0.000001) {
            setIdentity()
            return
        }

        val inv = 1f / n
        w *= inv; x *= inv; y *= inv; z *= inv
    }

    private fun multiplyRight(bw: Float, bx: Float, by: Float, bz: Float) {
        val nw = w * bw - x * bx - y * by - z * bz
        val nx = w * bx + x * bw + y * bz - z * by
        val ny = w * by - x * bz + y * bw + z * bx
        val nz = w * bz + x * by - y * bx + z * bw
        set(nw, nx, ny, nz)
    }

    private fun multiplyLeft(aw: Float, ax: Float, ay: Float, az: Float) {
        val nw = aw * w - ax * x - ay * y - az * z
        val nx = aw * x + ax * w + ay * z - az * y
        val ny = aw * y - ax * z + ay * w + az * x
        val nz = aw * z + ax * y - ay * x + az * w
        set(nw, nx, ny, nz)
    }

    override fun toString(): String {
        return "Quaternion(w=$w, x=$x, y=$y, z=$z)"
    }
}
