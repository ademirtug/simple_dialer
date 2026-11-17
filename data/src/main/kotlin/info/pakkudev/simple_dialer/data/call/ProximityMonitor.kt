package info.pakkudev.simple_dialer.data.call

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

interface ProximityMonitor {
    val available: Boolean
    val near: Flow<Boolean?>
}

class AndroidProximityMonitor(context: Context) : ProximityMonitor {
    private val manager = context.getSystemService(SensorManager::class.java)
    private val sensor = manager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    override val available get() = sensor != null
    override val near: Flow<Boolean?> = callbackFlow {
        trySend(null)
        val sensor = sensor
        if (sensor == null) { close(); return@callbackFlow }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) { trySend(event.values[0] < sensor.maximumRange) }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (!manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)) { close(); return@callbackFlow }
        awaitClose { manager.unregisterListener(listener) }
    }.distinctUntilChanged()
}
