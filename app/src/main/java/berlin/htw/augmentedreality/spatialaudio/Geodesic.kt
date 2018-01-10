package berlin.htw.augmentedreality.spatialaudio

import android.location.Location

// algorithms taken from https://www.sunearthtools.com/tools/distance.php
object Geodesic {
    val EARTH_RADIUS_KM = 6372.795477598

    val TO_RAD = Math.PI / 180

    /**
     * Returns distance between a and b in kilometers
     * Note that this function is ignoring the altitude of the Positions
     *
     * @param a     Array of length 2 with latitude and longitude
     * @param b     Array of length 2 with latitude and longitude
     */
    fun distanceBetween (a: Location, b: Location) =
            EARTH_RADIUS_KM * Math.acos(Math.sin(a.latitude * TO_RAD) * Math.sin(b.latitude * TO_RAD) + Math.cos(a.latitude * TO_RAD) * Math.cos(b.latitude * TO_RAD) * Math.cos(a.longitude * TO_RAD - b.longitude * TO_RAD))

    /**
     * Returns an angle between 0 an 2*PI, where 0 is the north pole and the angle turns counterclockwise
     * Note that this function is ignoring the altitude of the Positions
     *
     * @param from  Location
     * @param to    Location
     */
    fun direction (from: Location, to: Location): Double {
        val deltaPhi = Math.log(Math.tan(to.latitude * TO_RAD / 2 + Math.PI / 4) / Math.tan(from.latitude * TO_RAD / 2 + Math.PI / 4))
        // Note that we don't have don't take the abs here as described on the page in order to difference between "left of us" and "right of us":
        val deltaLon = (from.longitude - to.longitude) % 180
        return Math.atan2(deltaLon, deltaPhi)
    }

    fun directionToVector3(direction: Double): FloatArray {
        return floatArrayOf(
                Math.cos(direction) as Float,
                Math.sin(direction) as Float,
                0.0f
        )
    }
}