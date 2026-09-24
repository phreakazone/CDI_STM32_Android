package id.ns200.cdir7

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Pengelola notifikasi Status Bar sistem Android untuk IgniTra CDI R9.
 * Menyediakan notifikasi status koneksi, perubahan modul hardware,
 * dan peringatan keselamatan (safety lock).
 */
object CdiNotificationHelper {
    const val CHANNEL_ID = "cdi_status_channel"
    private const val NOTIFICATION_ID = 2001
    private const val ALERT_NOTIFICATION_ID = 2002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Status IgniTra CDI"
            val channelDescription = "Notifikasi status operasional dan peringatan keselamatan CDI"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = channelDescription
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        isAlert: Boolean = false
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }

            val iconRes = if (isAlert) {
                android.R.drawable.stat_sys_warning
            } else {
                android.R.drawable.stat_notify_sync
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(
                    if (isAlert) NotificationCompat.PRIORITY_HIGH
                    else NotificationCompat.PRIORITY_DEFAULT
                )
                .setAutoCancel(true)

            val manager = NotificationManagerCompat.from(context)
            val id = if (isAlert) ALERT_NOTIFICATION_ID else NOTIFICATION_ID
            manager.notify(id, builder.build())
        } catch (_: Exception) {
            // Tangani pengecualian secara aman jika izin dinonaktifkan sistem
        }
    }
}
