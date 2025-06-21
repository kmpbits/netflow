package com.kmpbits.sample.android.data.repository

import com.kmpbits.netflow_core.client.NetFlowClient
import com.kmpbits.netflow_core.deserializables.responseFlow
import com.kmpbits.netflow_core.deserializables.responseListFlow
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.states.ResultState
import com.kmpbits.netflow_core.states.map
import com.kmpbits.sample.android.data.database.AppDatabase
import com.kmpbits.sample.android.data.database.entity.TodoEntity
import com.kmpbits.sample.android.data.dto.TodoDto
import com.kmpbits.sample.android.data.mapper.toDto
import com.kmpbits.sample.android.data.mapper.toEntity
import com.kmpbits.sample.android.data.mapper.toModel
import com.kmpbits.sample.android.domain.model.Todo
import com.kmpbits.sample.android.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TodoRepositoryImpl(
    private val client: NetFlowClient,
    private val database: AppDatabase
) : TodoRepository {

    override fun getTodos(): Flow<ResultState<List<Todo>>> {
        return client.call {
            path = "todos"

        }.responseListFlow<TodoDto>{
            // Keep observing the local database
            // This only works if the localDatabase uses the TodoDto
            local({
                    observe {
                        database.todoDao().getTodos()
                    }
                }, { it.map(TodoEntity::toDto) }
            )

            // Here is the place to replace all the items in the local database
            onNetworkSuccess {
                database.todoDao().replaceTodos(it.map(TodoDto::toEntity))
            }
        }.map {
            it.map { it.map { it.toModel() } }
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
}