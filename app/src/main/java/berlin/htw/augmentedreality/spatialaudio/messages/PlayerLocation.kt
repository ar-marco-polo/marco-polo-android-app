package berlin.htw.augmentedreality.spatialaudio.messages

import android.location.Location

data class PlayerLocation(val latitude: Double, val longitude: Double, val accuracy: Float, val playerId: String?) {
    constructor (location: Location) : this(location.latitude, location.longitude, location.accuracy, null)
}