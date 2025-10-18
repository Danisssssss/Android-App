package com.example.usertracker.api

import com.example.usertracker.model.RemoteUser
import retrofit2.Response
import retrofit2.http.*

interface UserApiService {

    // JSONPlaceholder имеет endpoint /posts, используем его для имитации
    @GET("posts")
    suspend fun getUsers(): Response<List<RemoteUser>>

    @GET("posts/{id}")
    suspend fun getUser(@Path("id") id: Int): Response<RemoteUser>

    @POST("posts")
    suspend fun createUser(@Body user: RemoteUser): Response<RemoteUser>

    @PUT("posts/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body user: RemoteUser): Response<RemoteUser>

    @DELETE("posts/{id}")
    suspend fun deleteUser(@Path("id") id: Int): Response<Unit>
}