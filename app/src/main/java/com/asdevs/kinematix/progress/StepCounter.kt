package com.asdevs.kinematix.progress

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StepCounter(private val context: Context) : SensorEventListener {
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var initialSteps: Int = -1
    private var stepsListener: ((Int) -> Unit)? = null
    private val prefs: SharedPreferences = context.getSharedPreferences("step_prefs", Context.MODE_PRIVATE)

    fun start() {
        if (stepSensor == null) {
            // Device doesn't have a step counter sensor
            return
        }

        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        loadInitialSteps()
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun setStepUpdateListener(listener: (Int) -> Unit) {
        stepsListener = listener
    }

    private fun loadInitialSteps() {
        val today = getCurrentDate()
        val lastDate = prefs.getString("last_date", "")

        if (lastDate != today) {
            // New day, reset steps
            prefs.edit().apply {
                putString("last_date", today)
                putInt("daily_steps", 0)
                apply()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()

            if (initialSteps == -1) {
                initialSteps = totalSteps
            }

            val currentSteps = totalSteps - initialSteps
            prefs.edit().putInt("daily_steps", currentSteps).apply()
            stepsListener?.invoke(currentSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step counter
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Calendar.getInstance().time)
    }

    fun getTodaySteps(): Int {
        return prefs.getInt("daily_steps", 0)
    }
}