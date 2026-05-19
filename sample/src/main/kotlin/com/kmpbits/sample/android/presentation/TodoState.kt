package com.kmpbits.sample.android.presentation

data class TodoState(
    val title: String = "Add Todo",
    val todoTitle: String = "",
    val buttonTitle: String = "Add",
    val isChecked: Boolean = false,
    val isAddUpdateDialogVisible: Boolean = false,
)
