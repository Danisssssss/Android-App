package com.example.usertracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.usertracker.adapter.UserAdapter
import com.example.usertracker.model.User
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.usertracker.repository.UserRepository
import com.example.usertracker.localDB.UserUiState
import com.example.usertracker.localDB.UserViewModel
import com.example.usertracker.utils.FileUtils
import com.yourname.usertracker.network.SimpleNetworkMonitor
import kotlinx.coroutines.launch
import androidx.room.RoomDatabase
import com.example.usertracker.localDB.AppDatabase

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        initViews()
        setupRecyclerView()
        setupClickListeners()
        setupNetworkMonitoring()
        initRoomComponents()
        observeUsers()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    //region ЛР 1 работа с интерфейсом и ЛР 2 работа с разрешениями
    // Ссылка на витрину, которая показывает список
    private lateinit var usersRecyclerView: RecyclerView
    // Кнопка добавления нового пользователя
    private lateinit var fabAddUser: FloatingActionButton
    // Адаптер, главный посредник между данными и RecyclerView
    private lateinit var userAdapter: UserAdapter
    private var userIdCounter = 0

    // Launcher для получения результата из AddUserActivity
    private val addUserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                handleNewUserData(data)
            }
        }
    }

    private fun initViews() {
        usersRecyclerView = findViewById(R.id.usersRecyclerView)
        fabAddUser = findViewById(R.id.fabAddUser)
    }

    private fun setupRecyclerView() {
        // Создаём адаптер и передаем ему функцию-обработчик
        userAdapter = UserAdapter { user, isChecked ->
            // lambda-функция будет вызвана адаптером
            // когда пользователь нажмет на чекбокс в списке

            // Создаем обновленного пользователя
            val updatedUser = user.copy(
                isCompleted = isChecked,
                streak = if (isChecked) user.streak + 1 else user.streak
            )

            userViewModel.updateUser(updatedUser)

            val message = if (isChecked) {
                "${user.name} выполнена! Серия: ${user.streak + 1} дней"
            } else {
                "${user.name} не выполнена"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        usersRecyclerView.apply {
            // Размещаем элементы вертикальным список
            layoutManager = LinearLayoutManager(this@MainActivity)
            // Связываем адаптер с RecyclerView
            adapter = userAdapter
        }
    }

    private fun setupClickListeners() {
        fabAddUser.setOnClickListener {
            val intent = Intent(this, AddUser::class.java)
            addUserLauncher.launch(intent)
        }
    }

    // Добавление нового пользователя
    private fun handleNewUserData(data: Intent) {
        val name = data.getStringExtra("user_name") ?: ""
        val description = data.getStringExtra("user_description") ?: ""
        val imageUriString = data.getStringExtra("image_uri")
        val contactName = data.getStringExtra("contact_name")
        val contactPhone = data.getStringExtra("contact_phone")

        if (name.isEmpty()) {
            Toast.makeText(this, "Ошибка: имя пользователя пустое", Toast.LENGTH_SHORT).show()
            return
        }

        // Формируем описание с учетом выбранного контакта
        var fullDescription = description
        if (!contactName.isNullOrEmpty()) {
            fullDescription += if (fullDescription.isNotEmpty()) {
                "\n\nНапарник: $contactName"
            } else {
                "Напарник: $contactName"
            }
        }

        // Создаем нового пользователя
        val newUser = User(
            name = name,
            description = fullDescription,
            streak = 0,
            isCompleted = false,
            imageUri = imageUriString,
            buddyName = contactName,
            buddyPhone = contactPhone
        )

        // Сохраняем в БД
        //userViewModel.insertUser(newUser)
        userViewModel.addUserWithSync(newUser)

        // Показываем информацию о выбранных данных
        var message = "Пользователь \"$name\" добавлен!"
        if (!contactName.isNullOrEmpty()) {
            message += "\nНапарник: $contactName"
        }
        if (!imageUriString.isNullOrEmpty()) {
            message += "\nИзображение добавлено"
        }

        // Toast показывается автоматически через observeUsers()
    }
    //endregion

    //region ЛР 3 работа с сетью
    private lateinit var networkMonitor: SimpleNetworkMonitor

    private fun setupNetworkMonitoring() {
        // Создаем и запускаем мониторинг сети
        networkMonitor = SimpleNetworkMonitor(this)
        networkMonitor.startMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Останавливаем мониторинг сети при закрытии приложения
        networkMonitor.stopMonitoring()
    }
    //endregion

    //region ЛР 4 файловая система

    // Создаем меню
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_map -> {
                openMap()
                true
            }
            R.id.action_backup -> {
                createBackup()
                true
            }
            R.id.action_restore -> {
                restoreFromBackup()
                true
            }
            R.id.action_sync -> { // Новая кнопка синхронизации
                syncWithServer()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createBackup() {
        val currentUsers = userAdapter.currentList
        val success = FileUtils.saveUsersToFile(this, currentUsers)
        val message = if (success) {
            "Бэкап создан! Сохранено ${currentUsers.size} пользователей"
        } else {
            "Ошибка при создании бэкапа"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun restoreFromBackup() {
        if (!FileUtils.doesBackupExist(this)) {
            Toast.makeText(this, "Бэкап не найден", Toast.LENGTH_SHORT).show()
            return
        }

        val restoredUsers = FileUtils.loadUsersFromFile(this)
        if (restoredUsers != null) {
            // Удаляем все текущие привычки и добавляем восстановленные
            lifecycleScope.launch {
                userViewModel.users.collect { currentUsers ->
                    currentUsers.forEach { userViewModel.deleteUser(it) }
                }

                restoredUsers.forEach { userViewModel.insertUser(it) }
            }

            Toast.makeText(this, "Восстановлено ${restoredUsers.size} пользователей", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Ошибка при восстановлении", Toast.LENGTH_SHORT).show()
        }
    }
    //endregion

    //region ЛР 6 локальная БД
    // Room компоненты
    private lateinit var userViewModel: UserViewModel

    private fun initRoomComponents() {
        val database = AppDatabase.getInstance(this)
        val repository = UserRepository(database.userDao())
        userViewModel = UserViewModel(repository)
    }

    private fun observeUsers() {
        // Подписываемся на Flow из БД
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userViewModel.users.collect { users ->
                    userAdapter.setUser(users)

                    // Обновляем счетчик ID
                    if (users.isNotEmpty()) {
                        userIdCounter = users.maxByOrNull { it.id }?.id?.plus(1) ?: 1
                    }
                }
            }
        }

        // Наблюдаем за состоянием UI
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userViewModel.uiState.collect { state ->
                    when (state) {
                        is UserUiState.Success -> {
                            Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        is UserUiState.Error -> {
                            Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                        UserUiState.Loading -> {
                            // Можно показать ProgressBar
                        }
                    }
                }
            }
        }
    }
    //endregion

    //region ЛР 7 удаленная БД

    // Метод синхронизации
    private fun syncWithServer() {
        userViewModel.syncWithServer()
    }

    //endregion

    //region ЛР 8 карта

    private fun openMap() {
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
    }

    //endregion


}