package com.elee.eleeuilab.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
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
 * 基于android位置传感器实现，具体可以看google的官方开发文档
 * https://developer.android.google.cn/develop/sensors-and-location/sensors/sensors_position?hl=th#kotlin
 *
 * 目前addView必须用ShakeableLayoutParams
 */
class ShakeableLayerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    FrameLayout(context, attrs, defStyleAttr), DefaultLifecycleObserver {

    companion object {

        /**
         * 最大晃动角度
         */
        private const val MAX_SHAKE_ANGLE: Float = (PI / 9).toFloat()

        /**
         * 偏移动画时间
         * 因为传感器200ms才返回一次数据，如果没有动画就会一卡一卡的
         */
        private const val ANIM_TIME = 1000
    }

    /**
     * 传感器相关
     */
    private var sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    /**
     * 是否开始摇动
     */
    private var startShake = false

    /**
     * 开始摇动时角度
     */
    private val originOrientationAngles = FloatArray(3)

    /**
     * 偏移量比例和更新时间
     */
    private var verticalOffsetRatio = 0F
    private var horizontalOffsetRatio = 0F
    private var updateRatioTime: Long = 0L

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
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    init {
        /**
         * 传感器的启动关闭通过lifecycle监听，这样和activity解耦
         */
        if (context is LifecycleOwner) {
            (context as LifecycleOwner).lifecycle.addObserver(this)
        }
    }

    /**
     * 生成有最大偏移量的layoutParam
     */
    override fun generateLayoutParams(attrs: AttributeSet?): ShakeableLayoutParams {
        return ShakeableLayoutParams(context, attrs)
    }

    /**
     * 对于子view的偏移，直接通过平移动画布来实现
     *
     * p.s. 第一次这么玩，竟然成功了，还挺好玩
     */
    override fun drawChild(canvas: Canvas?, child: View?, drawingTime: Long): Boolean {
        val lp = child?.layoutParams as ShakeableLayoutParams?
        val maxOffset = lp?.maxOffset
        canvas?.save()
        // 计算实际偏移
        val animProcess =
            (SystemClock.elapsedRealtime() - updateRatioTime).coerceAtMost(ANIM_TIME.toLong()) / ANIM_TIME.toFloat()
        val hRatio =
            (1 - animProcess) * (lp?.currentHRatio ?: 0F) + animProcess * horizontalOffsetRatio
        val vRatio =
            (1 - animProcess) * (lp?.currentVRatio ?: 0F) + animProcess * verticalOffsetRatio
        lp?.currentHRatio = hRatio
        lp?.currentVRatio = vRatio
        maxOffset?.let {
            canvas?.translate(
                maxOffset * hRatio,
                -maxOffset * vRatio
            )
        }
        val result = super.drawChild(canvas, child, drawingTime)
        canvas?.restore()
        postInvalidate()
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
            //记录打开页面时角度
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
        // 记录更新时间
        updateRatioTime = SystemClock.elapsedRealtime()
    }
}

class ShakeableLayoutParams(context: Context, attrs: AttributeSet?) :
    FrameLayout.LayoutParams(context, attrs) {

    /**
     * 最大偏移量，单位dp
     */
    var maxOffset: Float = 0F

    /**
     * 当前偏移比例
     */
    var currentHRatio = 0F
    var currentVRatio = 0F

    init {
        val a: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ShakeableLayerLayout)
        maxOffset = a.getDimension(R.styleable.ShakeableLayerLayout_maxOffset, 0F)
        a.recycle()
    }
}