package berlin.htw.augmentedreality.spatialaudio

object Geodesic {
    val EARTH_RADIUS_KM = 6372.795477598

    /**
     * Returns an angle between -PI and PI, where 0 is the north pole and the angle turns counterclockwise
     * algorithm taken from http://www.movable-type.co.uk/scripts/latlong.html
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
     * Returns am Array of length 3 which indicates the direction to another position
     * in the Vector Space of the device
     *
     * @param bearing the bearing or azimuth between two positions
     */
    fun bearingToVector3(bearing: Double): FloatArray {
        return floatArrayOf(
                Math.sin(bearing).toFloat() * -1, // east / west axis
                Math.cos(bearing).toFloat(), // north / south axis
                0.0f // ground / sky axis
        )
    }
}