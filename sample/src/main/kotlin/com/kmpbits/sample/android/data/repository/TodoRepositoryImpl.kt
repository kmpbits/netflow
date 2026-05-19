package com.kmpbits.sample.android.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.map
import com.kmpbits.netflow_core.client.NetFlowClient
import com.kmpbits.netflow_core.deserializables.responseAsync
import com.kmpbits.netflow_core.deserializables.responseFlow
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_core.states.ResultState
import com.kmpbits.netflow_core.states.map
import com.kmpbits.netflow_paging.deserializable.responsePaginated
import com.kmpbits.sample.android.data.database.AppDatabase
import com.kmpbits.sample.android.data.dto.TodoDto
import com.kmpbits.sample.android.data.mapper.toDto
import com.kmpbits.sample.android.data.mapper.toEntity
import com.kmpbits.sample.android.data.mapper.toModel
import com.kmpbits.sample.android.data.source.LocalPagingSource
import com.kmpbits.sample.android.domain.model.Todo
import com.kmpbits.sample.android.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TodoRepositoryImpl(
    private val client: NetFlowClient,
    private val database: AppDatabase
) : TodoRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getTodos(): Flow<PagingData<Todo>> {
        val response = client.call {
            path = "todos"

        }

        return response.responsePaginated<TodoDto> {
            localSource(
                pagingSource = { LocalPagingSource(database.todoDao()) },
                transform = { it.toDto() }
            )

            deleteAll { database.todoDao().deleteTodos() }
            insertAll(transform = { it.toEntity() }) { database.todoDao().replaceTodos(it) }

            firstItemDatabase(
                itemDatabase = { database.todoDao().getTodos().first().firstOrNull() },
                timestamp = { it.lastUpdatedTimestamp }
            )
        }.map {
            it.map { it.toModel() }
        }
    }

    override fun addTodo(title: String, completed: Boolean): Flow<ResultState<Todo>> {
        return client.call {
            path = "todos"
            method = HttpMethod.Post

            body(
                mapOf(
                    "userId" to 1,
                    "title" to title,
                    "completed" to completed
                )
            )
        }.responseFlow<TodoDto> {
            onNetworkSuccess {
                // Here is the place to add the item to the local database
                database.todoDao().upsertTodo(it.toEntity())
            }
        }.map {
            it.map(TodoDto::toModel)
        }
    }

    override fun updateTodo(id: Int, title: String, completed: Boolean): Flow<ResultState<Todo>> {
        return client.call {
            path = "todos/$id"
            method = HttpMethod.Put

            body(
                mapOf(
                    "userId" to 1,
                    "title" to title,
                    "completed" to completed
                )
            )
        }.responseFlow<TodoDto> {
            onNetworkSuccess {
                // Here is the place to update the item in the local database
                database.todoDao().upsertTodo(it.toEntity())
            }

        }.map {
            it.map(TodoDto::toModel)
        }
    }

    override suspend fun deleteTodo(id: Int): AsyncState<Unit> {
        return client.call {
            path = "todos/$id"
            method = HttpMethod.Delete
        }.responseAsync {
            onNetworkSuccess {
                // Here is the place to delete the item in the local database
                database.todoDao().deleteTodo(id)
            }
        }
    }
}