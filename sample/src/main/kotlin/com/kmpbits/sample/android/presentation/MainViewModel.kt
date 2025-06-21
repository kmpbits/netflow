package com.kmpbits.sample.android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_core.states.ResultState
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
                    submitTodo(isUpdate = true, id = it)
                } ?: run {
                    submitTodo(isUpdate = false)
                }
            }
            TodoAction.DismissAddUpdateDialog -> updateDialogVisibility(false, null)
            is TodoAction.ShowAddUpdateDialog -> updateDialogVisibility(true, action.todo)
            is TodoAction.UpdateIsChecked -> updateIsChecked(action.isChecked)
            is TodoAction.UpdateTitle -> updateTitle(action.title)
            is TodoAction.UpdateTodoCheck -> updateTodoCheck(action.todo)
            is TodoAction.DeleteTodo -> delete(action.id)
        }
    }

    private fun updateTodoCheck(todo: Todo) {
        updateTitle(todo.title)
        updateIsChecked(!todo.completed) // This is needed because we are only changing the isChecked value
        submitTodo(isUpdate = true, id = todo.id)
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

    private fun submitTodo(isUpdate: Boolean, id: Int? = null) = viewModelScope.launch {
        val title = state.value.todoTitle
        val isChecked = state.value.isChecked

        val flow = if (isUpdate && id != null) {
            repository.updateTodo(id, title, isChecked)
        } else {
            repository.addTodo(title, isChecked)
        }

        flow.collectLatest { response ->
            when (response) {
                is ResultState.Success -> resetDialog()
                else -> {} // Optional: Handle loading/error
            }
        }
    }

    private fun resetDialog() {
        _state.update {
            it.copy(
                isAddUpdateDialogVisible = false,
                todoTitle = "",
                isChecked = false
            )
        }
    }

    private fun delete(id: Int) = viewModelScope.launch {
        when(repository.deleteTodo(id)) {
            AsyncState.Empty -> {}
            is AsyncState.Error -> {}
            is AsyncState.Success -> {
                // Don't need to do anything, since the local database will be updated automatically
            }
        }
    }
}