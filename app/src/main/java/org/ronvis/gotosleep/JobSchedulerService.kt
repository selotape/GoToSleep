package org.ronvis.gotosleep

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters


class NotifierWorker(private val ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        raiseNotification()
        return Result.success()
    }

    private fun raiseNotification() {
        val prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
        if (!prefs.getBoolean(ENABLED, true)) {
            return
        }

        with(NotificationManagerCompat.from(ctx)) {
            // notificationId is a unique int for each notification that you must define
            notify(123, createNotification().build())
        }
    }

    fun createNotification(): NotificationCompat.Builder {
//        val pendingIntent: PendingIntent =
//            PendingIntent.getActivity(ctx, 0, Intent.makeMainActivity(), PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(ctx, ctx.getString(R.string.channel_id))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(ctx.getString(R.string.notification_title))
            .setContentText(ctx.getString(R.string.notification_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
//            .setContentIntent(pendingIntent)
            .setAutoCancel(false)

    }
}