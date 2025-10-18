package com.example.usertracker.localDB

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.usertracker.repository.UserRepository
import com.example.usertracker.model.User
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    val users = repository.getAllUsers()

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    fun insertUser(user: User) {
        viewModelScope.launch {
            try {
                repository.insertUser(user)
                _uiState.value = UserUiState.Success("Привычка добавлена")
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error("Ошибка добавления: ${e.message}")
            }
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            try {
                repository.updateUser(user)
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error("Ошибка обновления: ${e.message}")
            }
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            try {
                repository.deleteUser(user)
                _uiState.value = UserUiState.Success("Привычка удалена")
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error("Ошибка удаления: ${e.message}")
            }
        }
    }

    //region Удаленная БД

    fun syncWithServer() {
        viewModelScope.launch {
            try {
                _uiState.value = UserUiState.Loading
                repository.syncWithServer()
                _uiState.value = UserUiState.Success("Синхронизация завершена")
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error("Ошибка синхронизации: ${e.message}")
            }
        }
    }

    fun addUserWithSync(user: User) {
        viewModelScope.launch {
            try {
                // Сначала сохраняем локально
                val localId = repository.insertUser(user)

                // Пытаемся синхронизировать с сервером
                val synced = repository.syncUserToServer(user)

                if (synced) {
                    _uiState.value = UserUiState.Success("Привычка добавлена и синхронизирована")
                } else {
                    _uiState.value = UserUiState.Success("Привычка добавлена локально (оффлайн)")
                }
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error("Ошибка: ${e.message}")
            }
        }
    }

    //endregion
}

sealed class UserUiState {
    object Loading : UserUiState()
    data class Success(val message: String) : UserUiState()
    data class Error(val message: String) : UserUiState()
}