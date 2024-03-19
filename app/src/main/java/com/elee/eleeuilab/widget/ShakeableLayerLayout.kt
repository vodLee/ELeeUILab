package com.elee.eleeuilab.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.elee.eleeuilab.R
import kotlin.math.PI

/**
 * 可晃动分层布局
 * shake shake~
 *
 * 基于android位置传感器实现
 *
 * 目前初版完成，但是卡顿很明显，需要考虑优化方案。
 */
class ShakeableLayerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    FrameLayout(context, attrs, defStyleAttr), DefaultLifecycleObserver {

    companion object {
        private const val DEFAULT_CHILD_GRAVITY = Gravity.TOP or Gravity.START

        /**
         * 最大晃动角度
         */
        private const val MAX_SHAKE_ANGLE: Float = (PI / 9).toFloat()
    }

    private var sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var startShake = false
    private val originOrientationAngles = FloatArray(3)

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(
                    event?.values,
                    0,
                    accelerometerReading,
                    0,
                    accelerometerReading.size
                )
            } else if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event?.values, 0, magnetometerReading, 0, magnetometerReading.size)
            }
            updateOrientationAngles()
            invalidate()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ShakeableLayoutParams {
        return ShakeableLayoutParams(context, attrs)
    }

    var verticalOffsetRatio = 0F
    var horizontalOffsetRatio = 0F

    override fun drawChild(canvas: Canvas?, child: View?, drawingTime: Long): Boolean {
        val lp = child?.layoutParams as ShakeableLayoutParams?
        val maxOffset = lp?.maxOffset
        canvas?.save()
        maxOffset?.let {
            canvas?.translate(
                maxOffset * horizontalOffsetRatio,
                -maxOffset * verticalOffsetRatio
            )
        }
        val result = super.drawChild(canvas, child, drawingTime)
        canvas?.restore()
        return result
    }

    override fun onResume(owner: LifecycleOwner) {

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                listener,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                listener,
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        sensorManager.unregisterListener(listener)
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    fun updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        // "rotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        if (!startShake && orientationAngles[0] != 0F) {
            startShake = true
            System.arraycopy(
                orientationAngles,
                0,
                originOrientationAngles,
                0,
                originOrientationAngles.size
            )
        } else {
            // 横纵方向偏移比例
            verticalOffsetRatio = if (startShake) {
                (orientationAngles[1] - originOrientationAngles[1])
                    .coerceAtLeast(-MAX_SHAKE_ANGLE)
                    .coerceAtMost(MAX_SHAKE_ANGLE) / MAX_SHAKE_ANGLE
            } else 0F
            horizontalOffsetRatio = if (startShake) {
                (orientationAngles[2] - originOrientationAngles[2])
                    .coerceAtLeast(-MAX_SHAKE_ANGLE)
                    .coerceAtMost(MAX_SHAKE_ANGLE) / MAX_SHAKE_ANGLE
            } else 0F
        }
    }
}

class ShakeableLayoutParams(context: Context, attrs: AttributeSet?) :
    FrameLayout.LayoutParams(context, attrs) {

    /**
     * 最大偏移量，单位dp
     */
    var maxOffset: Float = 0F

    init {
        val a: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ShakeableLayerLayout)
        maxOffset = a.getDimension(R.styleable.ShakeableLayerLayout_maxOffset, 0F)
        a.recycle()
    }
}