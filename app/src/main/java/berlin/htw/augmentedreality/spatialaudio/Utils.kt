package berlin.htw.augmentedreality.spatialaudio

object Utils {
    /**
     * Each quaternion [q] and [r] is expected to be an array of (w, x, y, z)
     */
    private fun hamiltonMultiplication(q: FloatArray, r: FloatArray) = floatArrayOf(
            r[0] * q[0] - r[1] * q[1] - r[2] * q[2] - r[3] * q[3],
            r[0] * q[1] + r[1] * q[0] - r[2] * q[3] + r[3] * q[2],
            r[0] * q[2] + r[1] * q[3] + r[2] * q[0] - r[3] * q[1],
            r[0] * q[3] - r[1] * q[2] + r[2] * q[1] + r[3] * q[0]
    )

    /**
     * Returns an array of x, y, z representing a 3D-Vector;
     * [q] is required to take the form (w, x, y, z) and [location] to take the form (x, y, z);
     * the result is an array of (x, y, z)
     */
    fun rotateByQuaternion (q: FloatArray, location: FloatArray): FloatArray {
        val asQuaternion = floatArrayOf(0.0f, location[0], location[1], location[2])
        val qConjugation = floatArrayOf(q[0], -q[1], -q[2], -q[3])
        val rotated = hamiltonMultiplication(hamiltonMultiplication(q, asQuaternion), qConjugation)
        return rotated.sliceArray(1..(rotated.size - 1))
    }

    fun lengthOf (vec: FloatArray): Double = Math.sqrt(vec.map { it * it }.sum().toDouble())

    fun dotProduct (a: FloatArray, b: FloatArray): Float = a.mapIndexed { i, _ -> a[i] * b[i] }.sum()

    fun radiansBetween (a: FloatArray, b: FloatArray): Double = Math.acos(dotProduct(a, b) / (lengthOf(a) * lengthOf(b)))

}