package com.example.usertracker.repository

import com.example.usertracker.localDB.UserDao
import com.example.usertracker.model.User
import com.example.usertracker.model.RemoteUser
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    suspend fun getUserById(id: Long): User? = userDao.getUserById(id)

    suspend fun insertUser(user: User): Long = userDao.insertUser(user)

    suspend fun updateUser(user: User) = userDao.updateUser(user)

    suspend fun deleteUser(user: User) = userDao.deleteUser(user)

    suspend fun deleteAllUsers() = userDao.deleteAllUsers()

    //region Удаленная БД
    private val remoteRepository = RemoteUserRepository()

    suspend fun syncWithServer() {
        try {
            // Получаем привычки с сервера
            val remoteUsers = remoteRepository.getUsersFromServer()

            // Сохраняем их в локальную БД
            remoteUsers.forEach { remoteUser ->
                val localUser = remoteUser.toLocalUser()
                userDao.insertUser(localUser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncUserToServer(user: User): Boolean {
        return try {
            val remoteUser = user.toRemoteUser()
            remoteRepository.syncUserToServer(remoteUser)
        } catch (e: Exception) {
            false
        }
    }
    //endregion
}

// Extension functions для конвертации между моделями
fun RemoteUser.toLocalUser(): User {
    return User(
        id = this.id,
        name = this.name,
        description = this.description,
        streak = this.streak,
        isCompleted = this.isCompleted,
        imageUri = this.imageUri,
        buddyName = this.buddyName,
        buddyPhone = this.buddyPhone,
    )
}

fun User.toRemoteUser(): RemoteUser {
    return RemoteUser(
        id = this.id,
        name = this.name,
        description = this.description,
        streak = this.streak,
        isCompleted = this.isCompleted,
        imageUri = this.imageUri,
        buddyName = this.buddyName,
        buddyPhone = this.buddyPhone,
    )
}