package com.example.usertracker.repository

import com.example.usertracker.api.UserApiService
import com.example.usertracker.api.RetrofitClient
import com.example.usertracker.model.RemoteUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class RemoteUserRepository {

    private val apiService: UserApiService = RetrofitClient.userApiService

    // Реальное получение данных с сервера
    suspend fun getUsersFromServer(): List<RemoteUser> = withContext(Dispatchers.IO) {
        try {
            val response: Response<List<RemoteUser>> = apiService.getUsers()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                // Если запрос неуспешен, возвращаем mock данные
                getMockUsers()
            }
        } catch (e: Exception) {
            // При ошибке сети возвращаем mock данные
            e.printStackTrace()
            getMockUsers()
        }
    }

    // Реальная отправка данных на сервер
    suspend fun syncUserToServer(user: RemoteUser): Boolean = withContext(Dispatchers.IO) {
        try {
            val response: Response<RemoteUser> = apiService.createUser(user)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Mock данные для fallback
    private fun getMockUsers(): List<RemoteUser> {
        return listOf(
            RemoteUser(
                id = 1001,
                name = "Серверная привычка 1",
                description = "Эта привычка пришла с сервера",
                streak = 7,
                isCompleted = true
            ),
            RemoteUser(
                id = 1002,
                name = "Серверная привычка 2",
                description = "Еще одна привычка с сервера",
                streak = 3,
                isCompleted = false
            )
        )
    }

    // Обновление привычки на сервере
    suspend fun updateUserOnServer(user: RemoteUser): Boolean = withContext(Dispatchers.IO) {
        try {
            val response: Response<RemoteUser> = apiService.updateUser(user.id, user)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Удаление привычки с сервера
    suspend fun deleteUserOnServer(userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val response: Response<Unit> = apiService.deleteUser(userId)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}