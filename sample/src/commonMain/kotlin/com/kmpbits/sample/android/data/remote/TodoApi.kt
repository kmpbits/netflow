package com.kmpbits.sample.android.data.remote

import androidx.paging.PagingData
import com.kmpbits.netflow_annotations.Body
import com.kmpbits.netflow_annotations.DELETE
import com.kmpbits.netflow_annotations.GET
import com.kmpbits.netflow_annotations.Headers
import com.kmpbits.netflow_annotations.Multipart
import com.kmpbits.netflow_annotations.NetFlowApi
import com.kmpbits.netflow_annotations.POST
import com.kmpbits.netflow_annotations.Paginated
import com.kmpbits.netflow_annotations.Part
import com.kmpbits.netflow_annotations.Path
import com.kmpbits.netflow_annotations.Query
import com.kmpbits.netflow_annotations.SkipAuth
import com.kmpbits.netflow_annotations.Wrapped
import com.kmpbits.netflow_core.builders.FilePart
import com.kmpbits.netflow_core.request.NetFlowCall
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_core.states.ResultState
import com.kmpbits.sample.android.data.dto.CreateTodoRequest
import com.kmpbits.sample.android.data.dto.TodoDto
import com.kmpbits.sample.android.data.dto.UploadResponse
import kotlinx.coroutines.flow.Flow

@NetFlowApi
interface TodoApi {

    @GET("todos")
    suspend fun getTodos(@Query completed: Boolean?): AsyncState<List<TodoDto>>

    @GET("todos/{id}")
    fun observeTodo(@Path id: Int): Flow<ResultState<TodoDto>>

    @POST("todos")
    suspend fun create(@Body payload: Map<String, Any>): AsyncState<TodoDto>

    @Headers("Accept: application/json", "X-Client: netflow")
    @POST("todos")
    suspend fun createTyped(@Body request: CreateTodoRequest): AsyncState<TodoDto>

    @Wrapped
    @GET("todos/{id}")
    suspend fun getWrapped(@Path id: Int): AsyncState<TodoDto>

    @DELETE("todos/{id}")
    suspend fun delete(@Path id: Int): AsyncState<Unit>

    @GET("todos")
    fun pagedTodos(): Flow<PagingData<TodoDto>>

    @Paginated(pageSize = 15)
    @GET("todos")
    fun pagedTodosSmall(): Flow<PagingData<TodoDto>>

    @SkipAuth
    @POST("login")
    suspend fun login(@Body request: CreateTodoRequest): AsyncState<TodoDto>

    @GET("todos/{id}")
    suspend fun getTodoModel(@Path id: Int): TodoDto            // -> responseToModel<TodoDto>()

    @GET("todos")
    suspend fun getTodoModels(): List<TodoDto>                  // -> responseToModel<List<TodoDto>>()

    @Multipart
    @POST("todos/{todoId}/attachments")
    suspend fun uploadTodoAttachment(
        @Path todoId: Int,
        @Part("caption") caption: String,
        @Part("file") file: FilePart,
        @Part("meta") meta: CreateTodoRequest?,
    ): AsyncState<UploadResponse>

    @GET("todos")
    suspend fun todosCallSuspend(): NetFlowCall                 // suspend + NetFlowCall

    @GET("todos")
    fun todosCall(): NetFlowCall
}
