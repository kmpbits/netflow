package com.kmpbits.sample.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmpbits.netflow_core.deserializables.responseListFlow
import com.kmpbits.netflow_core.extensions.netflowClient
import com.kmpbits.netflow_core.states.ResultState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow<ResultState<List<TodoDto>>>(ResultState.Empty)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getClient().call {
                path = "todos"
            }.responseListFlow<TodoDto>().collectLatest { response ->
                _state.value = response
            }
        }
    }

    private fun getClient() = netflowClient {
        baseUrl = "https://jsonplaceholder.typicode.com/"
    }
}