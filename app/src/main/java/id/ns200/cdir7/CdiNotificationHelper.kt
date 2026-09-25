package id.ns200.cdir7

import android.annotation.SuppressLint
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
    const val ENGINE_CHANNEL_ID = "cdi_engine_status_v2"
    const val SAFETY_CHANNEL_ID = "cdi_safety_alerts_v2"
    private const val ENGINE_NOTIFICATION_ID = 2001
    private const val ALERT_NOTIFICATION_ID = 2002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val engineChannel = NotificationChannel(
                ENGINE_CHANNEL_ID,
                "Status Mesin IgniTra",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hanya status mesin hidup atau mati"
                enableVibration(false)
            }
            val safetyChannel = NotificationChannel(
                SAFETY_CHANNEL_ID,
                "Peringatan Keselamatan IgniTra",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fault dan interlock keselamatan penting"
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannels(listOf(engineChannel, safetyChannel))
            /* Channel lama berisi notifikasi rutin yang bising. Hapus setelah
             * migrasi supaya konfigurasi importance lama tidak terbawa. */
            notificationManager?.deleteNotificationChannel("cdi_status_channel")
        }
    }

    private fun notificationsAllowed(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun showEngineStatus(context: Context, running: Boolean, rpm: Int) {
        try {
            if (!notificationsAllowed(context)) return
            val message = if (running) "Mesin hidup • $rpm RPM" else "Mesin mati"
            val builder = NotificationCompat.Builder(context, ENGINE_CHANNEL_ID)
                .setSmallIcon(if (running) android.R.drawable.presence_online else android.R.drawable.presence_offline)
                .setContentTitle("IgniTra • Status Mesin")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setOngoing(running)
                .setAutoCancel(!running)
            NotificationManagerCompat.from(context).notify(ENGINE_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Izin notifikasi dapat dicabut ketika aplikasi sedang berjalan.
        }
    }

    @SuppressLint("MissingPermission")
    fun showSafetyAlert(context: Context, message: String) {
        try {
            if (!notificationsAllowed(context)) return
            val builder = NotificationCompat.Builder(context, SAFETY_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Peringatan Keselamatan CDI")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
            NotificationManagerCompat.from(context).notify(ALERT_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Izin notifikasi dapat dicabut ketika aplikasi sedang berjalan.
        }
    }
}
