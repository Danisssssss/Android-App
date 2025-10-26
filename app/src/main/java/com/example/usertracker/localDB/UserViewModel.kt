package com.example.usertracker.localDB

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.usertracker.repository.UserRepository
import com.example.usertracker.model.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class UserViewModel(private val repository: UserRepository) : ViewModel() {
    // одноразовое событие: (name, profession)
    private val _userAdded = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 1)
    val userAdded: SharedFlow<Pair<String, String>> = _userAdded.asSharedFlow()

    val users = repository.getAllUsers()

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    fun insertUser(user: User) {
        viewModelScope.launch {
            try {
                val id = repository.insertUser(user)
                if (id > 0) {
                    _uiState.value = UserUiState.Success("Пользователь добавлен")

                    // В твоей модели НЕТ поля profession.
                    // Ты кладёшь профессию в description, а ещё можешь дописывать туда "Напарник: ..."
                    // Возьмём “чистую профессию” как часть ДО "Напарник: ".
                    val profession = (user.description ?: "").substringBefore("\n\nНапарник:")
                    _userAdded.tryEmit(user.name to profession)
                } else {
                    _uiState.value = UserUiState.Error("Не удалось добавить пользователя")
                }
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
                val localId = repository.insertUser(user)
                val synced = repository.syncUserToServer(user)

                _uiState.value = if (synced) {
                    UserUiState.Success("Пользователь добавлен и синхронизирован")
                } else {
                    UserUiState.Success("Пользователь добавлен локально (оффлайн)")
                }

                if (localId > 0) {
                    val profession = (user.description ?: "").substringBefore("\n\nНапарник:")
                    _userAdded.tryEmit(user.name to profession)
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