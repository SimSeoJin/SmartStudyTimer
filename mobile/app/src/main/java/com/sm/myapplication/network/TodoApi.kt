package com.sm.myapplication.network

import com.sm.myapplication.network.dto.TodoRequest
import com.sm.myapplication.network.dto.TodoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TodoApi {
    @GET("todos/{memberId}")
    suspend fun list(@Path("memberId") memberId: Long): Response<List<TodoResponse>>

    @POST("todos/{memberId}")
    suspend fun create(@Path("memberId") memberId: Long, @Body request: TodoRequest): Response<TodoResponse>

    @PUT("todos/{memberId}/{todoId}")
    suspend fun update(
        @Path("memberId") memberId: Long,
        @Path("todoId") todoId: Long,
        @Body request: TodoRequest,
    ): Response<TodoResponse>

    @PATCH("todos/{memberId}/{todoId}/done")
    suspend fun setDone(
        @Path("memberId") memberId: Long,
        @Path("todoId") todoId: Long,
        @Query("done") done: Boolean,
    ): Response<TodoResponse>

    @DELETE("todos/{memberId}/{todoId}")
    suspend fun delete(@Path("memberId") memberId: Long, @Path("todoId") todoId: Long): Response<Unit>
}
