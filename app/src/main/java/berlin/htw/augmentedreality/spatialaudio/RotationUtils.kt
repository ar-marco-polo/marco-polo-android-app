package berlin.htw.augmentedreality.spatialaudio

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

object RotationUtils {
    // the android os assumes our original device rotation to be laid down flat on the ground, facing the
    // geomagnetic north pole; we position our ears on the top left corner and top right corner of this phone
    val origin = arrayOf(
        floatArrayOf(-0.1f, 1.0f, 0.0f),
        floatArrayOf(0.1f, 1.0f, 0.0f)
    )

    private var isInitialized = false

    fun setup(ctx: Context) {
        if (isInitialized) return

        // set up sensor listener
        // see also: https://source.android.com/devices/sensors/sensor-types#rotation_vector
        val sensorManager = (ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager)

        // rotation vector (= Accelerometer, Magnetometer, and Gyroscope)
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(Rotation3DEventListener(), rotationSensor, SensorManager.SENSOR_DELAY_UI)

        // if we don't have a Gyroscope use Accelerometer, Magnetometer
        if (rotationSensor == null) {
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            sensorManager.registerListener(Rotation2DEventListener(), accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(Rotation2DEventListener(), magnetometer, SensorManager.SENSOR_DELAY_UI)
        }
        isInitialized = true
    }

    private class Rotation3DEventListener : SensorEventListener {
        override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        override fun onSensorChanged(event: SensorEvent) {
            val (x, y, z, w) = event.values
            val quaternion = floatArrayOf(w, x, y, z)

            // now we rotate the original ears by our device position
            val ears = origin.map { ear -> VectorUtils.rotateByQuaternion(quaternion, ear) }
            Game.handleRotationChange(ears)
        }
    }

    var gravity: FloatArray? = null
    var geomagnetic: FloatArray? = null
    private class Rotation2DEventListener : SensorEventListener {
        override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type === Sensor.TYPE_ACCELEROMETER) {
                gravity = event.values
            }
            if (event.sensor.type === Sensor.TYPE_MAGNETIC_FIELD) {
                geomagnetic = event.values
            }
            if (gravity != null && geomagnetic != null) {
                val R = FloatArray(9)
                val success = SensorManager.getRotationMatrix(R, FloatArray(9), gravity, geomagnetic)
                if (success) {
                    val ears = origin.map { _ -> floatArrayOf(R[3], R[4], 0f) }
                    Game.handleRotationChange(ears)
                }
            }
        }
    }
}