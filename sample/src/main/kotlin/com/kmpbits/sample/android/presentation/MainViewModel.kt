package com.kmpbits.sample.android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmpbits.netflow_core.states.ResultState
import com.kmpbits.sample.android.domain.model.Todo
import com.kmpbits.sample.android.domain.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ResultState<List<Todo>>>(ResultState.Empty)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getTodos().collectLatest { response ->
                _state.value = response
            }
        }
    }
}