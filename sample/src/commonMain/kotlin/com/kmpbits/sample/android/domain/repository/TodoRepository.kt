package com.kmpbits.sample.android.domain.repository

import androidx.paging.PagingData
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_core.states.ResultState
import com.kmpbits.sample.android.domain.model.Todo
import kotlinx.coroutines.flow.Flow

interface TodoRepository {

    fun getTodos(): Flow<PagingData<Todo>>

    fun addTodo(
        title: String,
        completed: Boolean
    ): Flow<ResultState<Todo>>

    fun updateTodo(
        id: Int,
        title: String,
        completed: Boolean
    ): Flow<ResultState<Todo>>

    suspend fun deleteTodo(id: Int): AsyncState<Unit>
}