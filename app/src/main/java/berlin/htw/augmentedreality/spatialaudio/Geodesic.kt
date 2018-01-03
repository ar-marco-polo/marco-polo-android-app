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

    /**
     * Returns an angle between 0 an 2*PI, where 0 is the north pole and the angle turns counterclockwise
     *
     * @param from  Array of length 2 with latitude and longitude
     * @param to    Array of length 2 with latitude and longitude
     */
    fun direction (from: DoubleArray, to: DoubleArray): Double {
        val deltaPhi = Math.log(Math.tan(to[0] * TO_RAD / 2 + Math.PI / 4) / Math.tan(from[0] * TO_RAD / 2 + Math.PI / 4))
        // Note that we don't have don't take the abs here as described on the page in order to difference between "left of us" and "right of us":
        val deltaLon = (from[1] - to[1]) % 180
        return Math.atan2(deltaLon, deltaPhi)
    }
}