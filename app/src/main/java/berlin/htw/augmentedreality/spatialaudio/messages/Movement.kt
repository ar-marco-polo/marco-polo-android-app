package berlin.htw.augmentedreality.spatialaudio.messages

import android.location.Location

data class Movement(val position: DoubleArray) {
    constructor (location: Location) : this(doubleArrayOf(location.latitude, location.longitude))
}