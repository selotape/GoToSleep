package org.ronvis.gotosleep

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit.MINUTES


const val SHARED_PREFS_NAME = "SHARED_PREFS_NAME"
const val ENABLED = "ENABLED_PREF"
const val TIME_MIN = "TIME_MIN" // HH:MM
const val TIME_HOUR = "TIME_HOUR" // HH:MM
const val FREQ_IN_MINUTES = "FREQ_IN_MINUTES"
const val NUM_NOTIFICATIONS = 100


class MainActivity : AppCompatActivity() {

    private val workManager: WorkManager = WorkManager.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.activity_main)
        checkOverlayPermission()
        startAnnoyingPopup()
//
//        createNotificationChannel()
//
//        val enableDisableToggle = findViewById<CheckBox>(R.id.enableDisableToggle)
//        val timeInput = findViewById<TimePicker>(R.id.turnOffTimeInput)
//        val freqInput = findViewById<NumberPicker>(R.id.turnOffFrequencyInput)
//        freqInput.minValue = 1
//        freqInput.maxValue = 10
//
//        val prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
//        val editor: SharedPreferences.Editor = prefs.edit()
//
//
//        enableDisableToggle.isChecked = prefs.getBoolean(ENABLED, true)
//        timeInput.hour = prefs.getInt(TIME_HOUR, 23)
//        timeInput.minute = prefs.getInt(TIME_MIN, 0)
//        freqInput.value = prefs.getInt(FREQ_IN_MINUTES, 5)
//
//        timeInput.setOnTimeChangedListener { _, hourOfDay, minute ->
//            editor.putInt(TIME_HOUR, hourOfDay)
//            editor.putInt(TIME_MIN, minute)
//            Log.i(TAG, "Time changed to ${hourOfDay}:${minute}")
//            editor.apply()
//            rescheduleAllNotifications()
//        }
//
//        freqInput.setOnValueChangedListener { _, oldVal, newVal ->
//            editor.putLong(FREQ_IN_MINUTES, newVal.toLong())
//            Log.i(TAG, "Frequency changed to every $newVal (from $oldVal)")
//            editor.apply()
//            rescheduleAllNotifications()
//        }
//
//        enableDisableToggle.setOnCheckedChangeListener { _: CompoundButton, enabled: Boolean ->
//            editor.putBoolean(ENABLED, enabled)
//            Log.i(TAG, "Enabled: $enabled")
//            editor.apply()
//        }
//        rescheduleAllNotifications()
    }

    // method to ask user to grant the Overlay permission
    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
//             send user to the device settings
            val overlayIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(overlayIntent)
        }
    }

    private fun startAnnoyingPopup() {
        // check if the user has already granted the Draw over other apps permission
        if (!Settings.canDrawOverlays(this)) return

        startForegroundService(Intent(this, AnnoyingPopupForegroundService::class.java))
    }

    // check for permission again when user grants it from
    // the device settings, and start the service
    override fun onResume() {
        super.onResume()
        startAnnoyingPopup()
    }

    private fun rescheduleAllNotifications() {
        val prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)

        val currentYear = LocalDateTime.now().year
        val currentMonth = LocalDateTime.now().month
        val currentDayOfMonth = LocalDateTime.now().dayOfMonth


        val freqMins = prefs.getLong(FREQ_IN_MINUTES, 5)
        val startHour = prefs.getInt(TIME_HOUR, 23)
        val startMin = prefs.getInt(TIME_MIN, 0)
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


            val notifyRequest = OneTimeWorkRequestBuilder<NotifierWorker>().setInitialDelay(
                minsUntilTargetTime, MINUTES
            ).build()
            workManager.beginUniqueWork(
                getString(R.string.worker_name) + n, REPLACE, notifyRequest
            ).enqueue()
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

