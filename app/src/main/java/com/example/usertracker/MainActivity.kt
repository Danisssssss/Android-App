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
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import com.example.usertracker.utils.FileUtils
import com.yourname.usertracker.network.SimpleNetworkMonitor

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        initViews()
        setupRecyclerView()
        setupClickListeners()
        setupNetworkMonitoring()
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

    // Временный список пользователей для демонстрации
    private val sampleUsers = mutableListOf(
        User(++userIdCounter, "Данис", "Техник 1-ой категории", 5, false),
        User(++userIdCounter, "Шамиль", "Инжерен", 12, true),
        User(++userIdCounter, "Донат", "Аналитик", 3, false),
        User(++userIdCounter, "Давид", "Инженер", 7, true),
    )

    // Launcher для получения результата из AddHabitActivity
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
        usersRecyclerView = findViewById(R.id.habitsRecyclerView)
        fabAddUser = findViewById(R.id.fabAddHabit)
    }

    private fun setupRecyclerView() {
        // Создаём адаптер и передаем ему функцию-обработчик
        userAdapter = UserAdapter { habit, isChecked ->
            // lambda-функция будет вызвана адаптером
            // когда пользователь нажмет на чекбокс в списке

            val updatedUser = habit.copy(
                isCompleted = isChecked,
                streak = if (isChecked) habit.streak + 1 else habit.streak
            )

            // Обновляем в нашем списке
            updateUserInList(updatedUser)

            // Обновляем в адаптере
            userAdapter.updateUser(updatedUser)

            val message = if (isChecked) {
                "${habit.name} выполнена! Серия: ${habit.streak + 1} дней"
            } else {
                "${habit.name} не выполнена"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        usersRecyclerView.apply {
            // Размещаем элементы вертикальным список
            layoutManager = LinearLayoutManager(this@MainActivity)
            // Связываем адаптер с RecyclerView
            adapter = userAdapter
        }

        // Устанавливаем начальный список
        userAdapter.setUser(sampleUsers)
    }

    private fun setupClickListeners() {
        fabAddUser.setOnClickListener {
            val intent = Intent(this, AddUser::class.java)
            addUserLauncher.launch(intent)
        }
    }

    // Добавление нового пользователя
    private fun handleNewUserData(data: Intent) {
        val name = data.getStringExtra("habit_name") ?: ""
        val description = data.getStringExtra("habit_description") ?: ""
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
            id = userIdCounter++,
            name = name,
            description = fullDescription,
            streak = 0,
            isCompleted = false,
            imageUri = imageUriString,
            buddyName = contactName,
            buddyPhone = contactPhone
        )

        // Добавляем в наш список
        sampleUsers.add(0, newUser)

        // Добавляем в адаптер (безопасно)
        userAdapter.addUser(newUser)

        // Показываем информацию о выбранных данных
        var message = "Пользователь \"$name\" добавлен!"
        if (!contactName.isNullOrEmpty()) {
            message += "\nНапарник: $contactName"
        }
        if (!imageUriString.isNullOrEmpty()) {
            message += "\nИзображение добавлено"
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun updateUserInList(updatedUser: User) {
        val index = sampleUsers.indexOfFirst { it.id == updatedUser.id }
        if (index != -1) {
            sampleUsers[index] = updatedUser
        }
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
            R.id.action_backup -> {
                createBackup()
                true
            }
            R.id.action_restore -> {
                restoreFromBackup()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createBackup() {
        val success = FileUtils.saveUsersToFile(this, sampleUsers)
        val message = if (success) {
            "Бэкап создан! Сохранено ${sampleUsers.size} пользователей"
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
            sampleUsers.clear()
            sampleUsers.addAll(restoredUsers)
            userAdapter.setUser(sampleUsers)

            Toast.makeText(this, "Восстановлено ${restoredUsers.size} пользователей", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Ошибка при восстановлении", Toast.LENGTH_SHORT).show()
        }
    }
    //endregion


}