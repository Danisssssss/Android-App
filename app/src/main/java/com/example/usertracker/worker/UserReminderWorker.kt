package com.example.usertracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.usertracker.NotificationService
import com.example.usertracker.localDB.AppDatabase
import com.example.usertracker.repository.UserRepository

class UserReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Получаем привычки из БД
            val database = AppDatabase.getInstance(applicationContext)
            val repository = UserRepository(database.userDao())
            val users = repository.getAllUsers()

            // Используем Flow для получения списка
            var usersList = emptyList<com.example.usertracker.model.User>()
            users.collect { usersList = it }

            // Показываем уведомление
            val notificationService = NotificationService(applicationContext)
            notificationService.showRandomUserReminder(usersList)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}