package com.kmpbits.sample.android.data.repository

import com.kmpbits.netflow_core.client.NetFlowClient
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_core.states.ResultState
import com.kmpbits.netflow_core.states.map
import com.kmpbits.sample.android.data.dto.CreateTodoRequest
import com.kmpbits.sample.android.data.mapper.toModel
import com.kmpbits.sample.android.data.remote.createTodoApi
import com.kmpbits.sample.android.domain.model.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Example: the annotation layer plus domain mapping. The annotated `TodoApi` returns
 * DTOs; this repository maps them to [Todo] with the `map` helpers from netflow-core.
 *
 * Calls that need Paging or cache hooks (`onNetworkSuccess` / `local {}`) stay on the
 * `call {}` DSL — see [TodoRepositoryImpl].
 */
class TodoApiRepositoryImpl(client: NetFlowClient) {

    private val api = client.createTodoApi()

    suspend fun getTodos(): AsyncState<List<Todo>> =
        api.getTodos(completed = null).map { dtos -> dtos.map { it.toModel() } }

    fun observeTodo(id: Int): Flow<ResultState<Todo>> =
        api.observeTodo(id).map { state -> state.map { it.toModel() } }

    suspend fun create(title: String, completed: Boolean): AsyncState<Todo> =
        api.createTyped(CreateTodoRequest(title, completed)).map { it.toModel() }

    suspend fun delete(id: Int): AsyncState<Unit> = api.delete(id)
}
