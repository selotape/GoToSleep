package org.ronvis.gotosleep

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.NumberPicker
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES


const val SHARED_PREFS_NAME = "SHARED_PREFS_NAME"
const val ENABLED = "ENABLED_PREF"
const val TIME_MIN = "TIME_MIN"
const val TIME_MIN_DEFAULT = 0
const val TIME_HOUR = "TIME_HOUR"
const val TIME_HOUR_DEFAULT = 23
const val FREQ_IN_MINUTES = "FREQ_IN_MINUTES"
const val FREQ_IN_MINUTES_DEFAULT = 3L
const val NUM_NOTIFICATIONS = 100
const val TAG = "GoToSleep"


class MainActivity : AppCompatActivity() {

    private val workManager = WorkManager.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.activity_main)
        createNotificationChannel()

        val enableDisableToggle = findViewById<CheckBox>(R.id.enableDisableToggle)
        val timeInput = findViewById<TimePicker>(R.id.turnOffTimeInput)
        val freqInput = findViewById<NumberPicker>(R.id.turnOffFrequencyInput)
        freqInput.minValue = 1
        freqInput.maxValue = 10

        val prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs.edit()


        enableDisableToggle.isChecked = prefs.getBoolean(ENABLED, true)
        timeInput.hour = prefs.getInt(TIME_HOUR, TIME_HOUR_DEFAULT)
        timeInput.minute = prefs.getInt(TIME_MIN, TIME_MIN_DEFAULT)
        freqInput.value = prefs.getInt(FREQ_IN_MINUTES, FREQ_IN_MINUTES_DEFAULT.toInt())

        timeInput.setOnTimeChangedListener { _, hourOfDay, minute ->
            editor.putInt(TIME_HOUR, hourOfDay)
            editor.putInt(TIME_MIN, minute)
            Log.i(TAG, "Time changed to ${hourOfDay}:${minute}")
            editor.apply()
            rescheduleAllWorkers()

        }

        freqInput.setOnValueChangedListener { _, oldVal, newVal ->
            editor.putLong(FREQ_IN_MINUTES, newVal.toLong())
            Log.i(TAG, "Frequency changed to every $newVal (from $oldVal)")
            editor.apply()
            rescheduleAllWorkers()
        }

        enableDisableToggle.setOnCheckedChangeListener { _: CompoundButton, enabled: Boolean ->
            editor.putBoolean(ENABLED, enabled)
            Log.i(TAG, "Enabled: $enabled")
            editor.apply()
        }
        rescheduleAllWorkers()
    }

    private fun rescheduleAllWorkers() {
        val prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)

        val currentYear = LocalDateTime.now().year
        val currentMonth = LocalDateTime.now().month
        val currentDayOfMonth = LocalDateTime.now().dayOfMonth

        val freqMins = prefs.getLong(FREQ_IN_MINUTES, FREQ_IN_MINUTES_DEFAULT)
        val startHour = prefs.getInt(TIME_HOUR, TIME_HOUR_DEFAULT)
        val startMin = prefs.getInt(TIME_MIN, TIME_MIN_DEFAULT)
        val startTime = LocalDateTime.of(
            currentYear, currentMonth, currentDayOfMonth, startHour, startMin
        )

        val currentTime = LocalDateTime.now()


        for (n in 0..NUM_NOTIFICATIONS) {


            var targetDateTime = startTime.plusMinutes(freqMins * n)
            if (targetDateTime < currentTime) {
                targetDateTime = targetDateTime.plusDays(1)
            }

            val minsUntilTargetTime = currentTime.until(targetDateTime, ChronoUnit.MINUTES)

            val annoyRequest = PeriodicWorkRequestBuilder<AnnoyWorker>(1, TimeUnit.DAYS).setInitialDelay(
                minsUntilTargetTime, MINUTES
            ).build()

            val workName = getString(R.string.worker_name) + n
            Log.i(TAG, "Scheduling annoyRequest $workName for $startTime")
            workManager.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.REPLACE, annoyRequest)

        }
    }


    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val channelName = getString(R.string.channel_name)
        val channelId = getString(R.string.channel_id)
        val channelDescription = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = channelDescription
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

