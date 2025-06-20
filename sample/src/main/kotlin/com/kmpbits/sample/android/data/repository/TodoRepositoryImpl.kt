package com.kmpbits.sample.android.data.repository

import com.kmpbits.netflow_core.client.NetFlowClient
import com.kmpbits.netflow_core.deserializables.responseListFlow
import com.kmpbits.netflow_core.states.ResultState
import com.kmpbits.netflow_core.states.map
import com.kmpbits.sample.android.data.dto.TodoDto
import com.kmpbits.sample.android.data.mapper.toModel
import com.kmpbits.sample.android.domain.model.Todo
import com.kmpbits.sample.android.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TodoRepositoryImpl(
    private val client: NetFlowClient
) : TodoRepository {

    override fun getTodos(): Flow<ResultState<List<Todo>>> {
        return client.call {
            path = "todos"
        }.responseListFlow<TodoDto>().map {
            it.map { it.map { it.toModel() } }
        }
    }
}