package com.kmpbits.sample.android.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import com.kmpbits.netflow_core.client.NetFlowClient
import com.kmpbits.netflow_core.deserializables.responseAsync
import com.kmpbits.netflow_core.deserializables.responseFlow
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_core.states.ResultState
import com.kmpbits.netflow_paging.deserializable.responsePaginated
import com.kmpbits.sample.android.data.database.AppDatabase
import com.kmpbits.sample.android.data.dto.TodoDto
import com.kmpbits.sample.android.data.mapper.toEntity
import com.kmpbits.sample.android.data.mapper.toModel
import com.kmpbits.sample.android.data.source.TodoPagingSource
import com.kmpbits.sample.android.domain.model.Todo
import com.kmpbits.sample.android.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow

class TodoRepositoryImpl(
    private val client: NetFlowClient,
    private val database: AppDatabase
) : TodoRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getTodos(): Flow<PagingData<Todo>> {
        return client.call {
            path = "todos"
        }.responsePaginated<TodoDto, Todo> {
            localSource(
                pagingSource = { TodoPagingSource(database) },
                transform = { it.toModel() }
            )

            deleteOnRefresh = false
            insertAll(transform = { it.toEntity() }) { todos ->
                database.todoQueries.transaction {
                    database.todoQueries.deleteTodos()
                    todos.forEach { database.todoQueries.insertTodo(it) }
                }
            }

            firstItemDatabase(
                itemDatabase = { database.todoQueries.getFirstTodo().executeAsOneOrNull() },
                timestamp = { it.lastUpdatedTimestamp }
            )
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
        }.responseFlow<TodoDto, Todo>(transform = { it.toModel() }) {
            onNetworkSuccess { database.todoQueries.insertTodo(it.toEntity()) }
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
        }.responseFlow<TodoDto, Todo>(transform = { it.toModel() }) {
            onNetworkSuccess { database.todoQueries.insertTodo(it.toEntity()) }
        }
    }

    override suspend fun deleteTodo(id: Int): AsyncState<Unit> {
        return client.call {
            path = "todos/$id"
            method = HttpMethod.Delete
        }.responseAsync<Unit> {
            onNetworkSuccess { database.todoQueries.deleteTodo(id.toLong()) }
        }
    }
}
