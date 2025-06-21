package com.kmpbits.sample.android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmpbits.netflow_core.states.ResultState
import com.kmpbits.sample.android.core.utils.ListOperation
import com.kmpbits.sample.android.core.utils.modifyListById
import com.kmpbits.sample.android.domain.model.Todo
import com.kmpbits.sample.android.domain.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TodoState())
    val state: StateFlow<TodoState> = _state.asStateFlow()

    init {
        getTodos()
    }

    fun onAction(action: TodoAction) {
        when (action) {
            is TodoAction.AddUpdateTodo -> {
                action.id?.let {
                    //updateTodo(it)
                } ?: run {
                    addTodo()
                }
            }
            TodoAction.DismissAddUpdateDialog -> updateDialogVisibility(false, null)
            is TodoAction.ShowAddUpdateDialog -> updateDialogVisibility(true, action.todo)
            is TodoAction.UpdateIsChecked -> updateIsChecked(action.isChecked)
            is TodoAction.UpdateTitle -> updateTitle(action.title)
            is TodoAction.UpdateTodoCheck -> updateTodoCheck(action.todo)
            is TodoAction.DeleteTodo -> {}
        }
    }

    private fun updateTodoCheck(todo: Todo) {
        updateTitle(todo.title)
        updateIsChecked(!todo.completed) // This is needed because we are only changing the isChecked value
        //updateTodo(todo.id)
    }

    private fun updateDialogVisibility(isVisible: Boolean, todo: Todo?) {
        _state.update {
            it.copy(
                isAddUpdateDialogVisible = isVisible,
                title = if (todo == null) "Add Todo" else "Update Todo",
                buttonTitle = if (todo == null) "Add" else "Update",
                todoTitle = todo?.title ?: "",
                isChecked = todo?.completed ?: false
            )
        }
    }

    private fun updateTitle(title: String) {
        _state.update {
            it.copy(
                todoTitle = title
            )
        }
    }

    private fun updateIsChecked(isChecked: Boolean) {
        _state.update {
            it.copy(
                isChecked = isChecked
            )
        }
    }

    private fun getTodos() = viewModelScope.launch {
        repository.getTodos().collectLatest { responseState ->
            _state.update {
                it.copy(
                    todoListState = responseState
                )
            }
        }
    }

    // This API is onlu for testing purpose. On new fetch, the new added Todo will be gone
    private fun addTodo() = viewModelScope.launch {
        val title = state.value.todoTitle
        val isChecked = state.value.isChecked

        repository.addTodo(title, isChecked).collectLatest { response ->
            when (response) {
                is ResultState.Error -> {} // Not needed for this demo
                ResultState.Loading -> {} // Not needed for this demo
                is ResultState.Success -> {
                    val successState = state.value.todoListState as? ResultState.Success
                    val newItem = response.data

                    val newList = successState?.data?.let {
                        modifyListById(
                            list = it,
                            id = response.data.id,
                            operation = ListOperation.ADD,
                            idSelector = { it.id },
                            newItem = newItem
                        )
                    } ?: emptyList()

                    _state.update {
                        it.copy(
                            todoListState = ResultState.Success(newList),
                            isAddUpdateDialogVisible = false,
                            todoTitle = "",
                            isChecked = false
                        )
                    }
                }

                ResultState.Empty -> {}
            }
        }
    }

    // This API is onlu for testing purpose. On new fetch, the new update Todo will be reverted
//    private fun updateTodo(id: Int) = viewModelScope.launch {
//        val title = state.value.todoTitle
//        val isChecked = state.value.isChecked
//
//        repository.updateTodo(
//            id = id,
//            title = title,
//            completed = isChecked
//        ).collectLatest { response ->
//            when (response) {
//                is ResponseState.Error -> {} // Not needed for this demo
//                ResponseState.Loading -> {} // Not needed for this demo
//                is ResponseState.Success -> {
//                    val successState = state.value.todoListState as? ResponseState.Success
//                    val updatedItem = response.data
//                    val newList = successState?.data?.let {
//                        modifyListById(
//                            list = it,
//                            id = id,
//                            operation = ListOperation.UPDATE,
//                            idSelector = { it.id },
//                            newItem = updatedItem
//                        )
//
//                    } ?: listOf()
//
//                    _state.update {
//                        it.copy(
//                            todoListState = ResponseState.Success(newList),
//                            isAddUpdateDialogVisible = false,
//                            todoTitle = "",
//                            isChecked = false
//                        )
//                    }
//                }
//            }
//        }
//    }
//
//    private fun delete(id: Int) = viewModelScope.launch {
//        repository.deleteTodo(id).collectLatest { response ->
//            when (response) {
//                is ResponseState.Error -> {} // Not needed for this demo
//                ResponseState.Loading -> {} // Not needed for this demo
//                is ResponseState.Success -> {
//                    val successState = state.value.todoListState as? ResponseState.Success
//                    val newList = successState?.data?.let {
//                        modifyListById(
//                            list = it,
//                            id = id,
//                            operation = ListOperation.DELETE,
//                            idSelector = { it.id }
//                        )
//                    } ?: emptyList()
//
//                    _state.update {
//                        it.copy(
//                            todoListState = ResponseState.Success(newList),
//                            isAddUpdateDialogVisible = false,
//                            todoTitle = "",
//                            isChecked = false
//                        )
//                    }
//                }
//            }
//        }
//    }
}