package com.kmpbits.sample.android.presentation

import com.kmpbits.netflow_core.states.ResultState
import com.kmpbits.sample.android.domain.model.Todo

data class TodoState(
    val todoListState: ResultState<List<Todo>> = ResultState.Empty,
    val title: String = "Add Todo",
    val todoTitle: String = "",
    val buttonTitle: String = "Add",
    val isChecked: Boolean = false,
    val isAddUpdateDialogVisible: Boolean = false,
)
