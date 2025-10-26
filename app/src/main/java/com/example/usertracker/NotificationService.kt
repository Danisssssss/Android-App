package com.example.usertracker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.usertracker.model.User

class NotificationService(private val context: Context) {

    fun showRandomUserReminder(users: List<User>) {
        // Фильтруем невыполненные привычки
        val incompleteUsers = users.filter { !it.isCompleted }

        if (incompleteUsers.isNotEmpty()) {
            // Выбираем случайного пользователя  для напоминания
            val randomUser = incompleteUsers.random()
            showNotification(randomUser)
        } else {
            // Если все привычки выполнены - показываем ободряющее уведомление
            showMotivationalNotification()
        }
    }

    private fun showNotification(user: User) {
        val notification = NotificationCompat.Builder(context, MainActivity.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📋 Напоминание о случайном пользователе")
            .setContentText("Не забудьте: ${user.name}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${user.name}\n\n${user.description}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(MainActivity.NOTIFICATION_ID, notification)
    }

    private fun showMotivationalNotification() {
        val motivationalMessages = listOf(
            "Отличная работа!",
            "Вы молодец! Продолжайте в том же духе!",
            "Потрясающе! Не останавливайтесь!"
        )

        val randomMessage = motivationalMessages.random()

        val notification = NotificationCompat.Builder(context, MainActivity.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🎯 Пользователи")
            .setContentText(randomMessage)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(MainActivity.NOTIFICATION_ID + 1, notification)
    }

    fun showUserCompletedNotification(user: User) {
        val notification = NotificationCompat.Builder(context, MainActivity.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("✅ Привычка выполнена!")
            .setContentText("${user.name} - серия: ${user.streak} дней")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(user.id.toInt(), notification) // Уникальный ID для каждой привычки
    }
}