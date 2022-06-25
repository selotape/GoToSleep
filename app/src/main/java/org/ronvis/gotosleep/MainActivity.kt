package org.ronvis.gotosleep

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.NumberPicker
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


const val ENABLED = "ENABLED_PREF"
const val TIME_MIN = "TIME_MIN" // HH:MM
const val TIME_HOUR = "TIME_HOUR" // HH:MM
const val FREQ_IN_MINUTES = "FREQ_IN_MINUTES"
const val TAG = "GoToSleep"


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.activity_main)
        createNotificationChannel()

        val enableDisableToggle = findViewById<CheckBox>(R.id.enableDisableToggle)
        val timeInput = findViewById<TimePicker>(R.id.turnOffTimeInput)
        val freqInput = findViewById<NumberPicker>(R.id.turnOffFrequencyInput)
        freqInput.minValue = 1
        freqInput.maxValue = 30

        val prefs = getPreferences(MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs.edit()


        enableDisableToggle.isChecked = prefs.getBoolean(ENABLED, true)
        timeInput.hour = prefs.getInt(TIME_HOUR, 23)
        timeInput.minute = prefs.getInt(TIME_MIN, 0)
        freqInput.value = prefs.getInt(FREQ_IN_MINUTES, 5)

        timeInput.setOnTimeChangedListener { _, hourOfDay, minute ->
            editor.putInt(TIME_HOUR, hourOfDay)
            editor.putInt(TIME_MIN, minute)
            Log.i(TAG, "Time changed to ${hourOfDay}:${minute}")
            editor.apply()
        }

        freqInput.setOnValueChangedListener { _, oldVal, newVal ->
            editor.putInt(FREQ_IN_MINUTES, newVal)
            Log.i(TAG, "Frequency changed to every $newVal (from $oldVal)")
            editor.apply()
        }

        enableDisableToggle.setOnCheckedChangeListener { _: CompoundButton, enabled: Boolean ->
            editor.putBoolean(ENABLED, enabled)
            Log.i(TAG, "Enabled: $enabled")
            editor.apply()
        }

        raiseNotification()

    }

    private fun raiseNotification() {
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(123, createNotification().build())
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

    fun createNotification(): NotificationCompat.Builder {
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, getString(R.string.channel_id))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)

    }
}

