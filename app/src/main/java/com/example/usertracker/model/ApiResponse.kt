package com.example.usertracker.model

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)
