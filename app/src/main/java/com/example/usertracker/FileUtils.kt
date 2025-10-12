package com.example.usertracker.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.usertracker.model.User
import java.io.*

class FileUtils {
    companion object {
        private const val BACKUP_FILENAME = "habits_backup.json"

        // Сохраняем пользователя в JSON-файл
        fun saveUsersToFile(context: Context, users: List<User>): Boolean {
            return try {
                val gson = Gson()
                val jsonString = gson.toJson(users)

                val file = File(context.filesDir, BACKUP_FILENAME)
                file.writeText(jsonString)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        // Загружаем пользователей из JSON-файла
        fun loadUsersFromFile(context: Context): List<User>? {
            return try {
                val file = File(context.filesDir, BACKUP_FILENAME)
                if (!file.exists()) return null

                val jsonString = file.readText()
                val gson = Gson()
                val type = object : TypeToken<List<User>>() {}.type
                gson.fromJson<List<User>>(jsonString, type)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        // Проверяем существует ли бэкап
        fun doesBackupExist(context: Context): Boolean {
            val file = File(context.filesDir, BACKUP_FILENAME)
            return file.exists()
        }
    }
}