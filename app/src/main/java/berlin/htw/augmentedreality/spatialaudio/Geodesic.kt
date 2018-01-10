package berlin.htw.augmentedreality.spatialaudio

// algorithms taken from https://www.sunearthtools.com/tools/distance.php
object Geodesic {
    val EARTH_RADIUS_KM = 6372.795477598

    val TO_RAD = Math.PI / 180

    /**
     * Returns distance between a and b in kilometers
     *
     * @param a     Array of length 2 with latitude and longitude
     * @param b     Array of length 2 with latitude and longitude
     */
    fun distanceBetween (a: DoubleArray, b: DoubleArray) =
            EARTH_RADIUS_KM * Math.acos(Math.sin(a[0] * TO_RAD) * Math.sin(b[0] * TO_RAD) + Math.cos(a[0] * TO_RAD) * Math.cos(b[0] * TO_RAD) * Math.cos(a[1] * TO_RAD - b[1] * TO_RAD))

    // from: https://gis.stackexchange.com/questions/79633/how-to-determine-vector-between-two-lat-lon-points
    /**
     * Returns an angle between -PI and PI, where 0 is the north pole and the angle turns counterclockwise
     *
     * @param from  Array of length 2 with latitude and longitude
     * @param to    Array of length 2 with latitude and longitude
     */
    fun bearing (from: DoubleArray, to: DoubleArray): Double {
        val dLon = to[1] - from[1]
        var y = Math.sin(dLon) * Math.cos(to[0])
        var x = Math.cos(from[0]) * Math.sin(to[0]) -
                Math.sin(from[0]) * Math.cos(to[0]) * Math.cos(dLon)
        return Math.atan2(y, x)
    }

    /**
     * Returns am Array of length 3 in the Vector Space of the device
     */
    fun bearingToVector3(bearing: Double): FloatArray {
        return floatArrayOf(
                Math.sin(bearing).toFloat() * -1, // east / west axis
                Math.cos(bearing).toFloat(), // north / south axis
                0.0f // ground / sky axis
        )
    }
}