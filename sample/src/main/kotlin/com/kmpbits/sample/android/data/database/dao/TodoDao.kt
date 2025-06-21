package com.kmpbits.sample.android.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.kmpbits.sample.android.data.database.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos")
    fun getTodos(): Flow<List<TodoEntity>>

    @Upsert
    suspend fun upsertTodo(todo: TodoEntity)

    @Upsert
    suspend fun upsertTodos(todos: List<TodoEntity>)

    @Transaction
    suspend fun replaceTodos(todos: List<TodoEntity>) {
        deleteTodos()
        upsertTodos(todos)
    }

    @Query("DELETE FROM todos")
    suspend fun deleteTodos()
}