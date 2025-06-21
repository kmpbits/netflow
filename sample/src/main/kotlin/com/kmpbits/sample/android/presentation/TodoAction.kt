package com.kmpbits.sample.android.presentation

import com.kmpbits.sample.android.domain.model.Todo

sealed interface TodoAction {
    data class UpdateTitle(val title: String) : TodoAction
    data class UpdateIsChecked(val isChecked: Boolean) : TodoAction
    data class ShowAddUpdateDialog(val todo: Todo? = null) : TodoAction
    data object DismissAddUpdateDialog : TodoAction
    data class AddUpdateTodo(val id: Int? = null) : TodoAction
    data class UpdateTodoCheck(val todo: Todo) : TodoAction
    data class DeleteTodo(val id: Int) : TodoAction
}