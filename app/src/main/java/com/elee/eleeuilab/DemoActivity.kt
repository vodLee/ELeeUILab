package com.elee.eleeuilab

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.acos
import kotlin.math.sin

class DemoActivity : AppCompatActivity() {

    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                object : SensorEventListener{
                    override fun onSensorChanged(event: SensorEvent?) {
                        System.arraycopy(event?.values, 0, accelerometerReading, 0, accelerometerReading.size)
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    }
                },
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                object : SensorEventListener{
                    override fun onSensorChanged(event: SensorEvent?) {
                        System.arraycopy(event?.values, 0, magnetometerReading, 0, magnetometerReading.size)
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    }
                },
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        updateOrientationAngles()
    }

    val tick = Runnable {
        updateOrientationAngles()
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    fun updateOrientationAngles() {
        window.decorView.postDelayed(this.tick, 500)
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        // "rotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // "orientationAngles" now has up-to-date information.
        // 旋转角、俯仰角、旋转度
        // 俯仰角 平行于设备屏幕的平面与平行于地面的平面之间的角度 水平是0 头朝上是-90 头朝下是90，
        // 旋转度，正面朝上是0，右倾为正，左倾为负，正面朝下为+-180
        Log.e("ELEE", "orientationAngles = [${(orientationAngles[0] / Math.PI * 180).toInt()}, ${(orientationAngles[1] / Math.PI * 180).toInt()}, ${(orientationAngles[2] / Math.PI * 180).toInt()}")
    }
}