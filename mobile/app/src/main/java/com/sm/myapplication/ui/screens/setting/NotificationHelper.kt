package com.sm.myapplication.ui.screens.setting

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sm.myapplication.R

object NotificationHelper {
    private const val CHANNEL_ID = "study_notifications"
    private const val CHANNEL_NAME = "공부 알림"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "공부 타이머와 학습 관련 알림"
            }
        )
    }

    fun requestNotificationPermission(context: Context) {
        ensureChannel(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 && context is android.app.Activity) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    fun notifyStudyFinished(context: Context, durationText: String) {
        ensureChannel(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(
            2001,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("공부 기록이 저장됐어요")
                .setContentText("오늘 공부한 시간 $durationText")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
        )
    }
}
