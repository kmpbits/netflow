package com.kmpbits.sample.android.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.kmpbits.sample.android.data.dto.TodoDto
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos")
    fun getTodos(): Flow<List<TodoDto>>

    @Upsert
    suspend fun upsertTodo(todo: TodoDto)

    @Upsert
    suspend fun upsertTodos(todos: List<TodoDto>)

    @Transaction
    suspend fun replaceTodos(todos: List<TodoDto>) {
        deleteTodos()
        upsertTodos(todos)
    }

    @Query("DELETE FROM todos")
    suspend fun deleteTodos()
}