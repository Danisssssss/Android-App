package com.example.usertracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.usertracker.R
import com.example.usertracker.model.User
import androidx.core.net.toUri

// ListAdapter автоматически дает:
// - submitList() - безопасное обновление данных
// - DiffUtil - умное сравнение списков
// - Автоматические анимации изменений
// - Потокобезопасность
class UserAdapter(private val onUserChecked: (User, Boolean) -> Unit)
    : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    // ViewHolder класс
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivUserIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvDescription: TextView = itemView.findViewById(R.id.tvUserDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)

        // Заполняем данные
        holder.tvName.text = user.name
        holder.tvDescription.text = user.description

        // Отображаем изображение если есть
        user.imageUri?.let { uriString ->
            try {
                val uri = uriString.toUri()
                holder.ivIcon.setImageURI(uri)
            } catch (e: Exception) {
                holder.ivIcon.setImageResource(android.R.drawable.ic_menu_edit)
                e.printStackTrace()
            }
        } ?: run {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_edit)
        }
    }

    // Метод для обновления отдельного пользователя
    fun updateUser(updatedUser: User) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedUser.id }
        if (index != -1) {
            currentList[index] = updatedUser
            submitList(currentList)
        }
    }

    // Метод для добавления нового пользователя
    fun addUser(newUser: User) {
        val currentList = currentList.toMutableList()
        currentList.add(0, newUser) // Добавляем в начало
        submitList(currentList)
    }

    // Метод для полной замены списка
    fun setUser(users: List<User>) {
        submitList(users.toList())
    }
}

// DiffUtil для эффективного обновления списка
class UserDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
}