package com.kmpbits.sample.android.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.kmpbits.sample.android.presentation.components.AddUpdateTodoDialog
import com.kmpbits.sample.android.presentation.components.TodoItem
import com.kmpbits.sample.android.presentation.theme.MyApplicationTheme
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel = koinViewModel<MainViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val todos = viewModel.todos.collectAsLazyPagingItems()

            var idClicked by remember {
                mutableStateOf<Int?>(null)
            }

            if (state.isAddUpdateDialogVisible) {
                AddUpdateTodoDialog(
                    onDismiss = { viewModel.onAction(TodoAction.DismissAddUpdateDialog) },
                    dialogTitle = state.title,
                    todoTitle = state.todoTitle,
                    buttonTitle = state.buttonTitle,
                    onAddToDo = { viewModel.onAction(TodoAction.AddUpdateTodo(idClicked)) },
                    onTodoUpdate = { viewModel.onAction(TodoAction.UpdateTitle(it)) },
                    isChecked = state.isChecked,
                    onCheckedChange = { viewModel.onAction(TodoAction.UpdateIsChecked(it)) }
                )
            }

            MyApplicationTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Todo List") }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                idClicked = null
                                viewModel.onAction(TodoAction.ShowAddUpdateDialog())
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Todo"
                            )
                        }
                    }
                ) { values ->
                    when (todos.loadState.refresh) {
                        is LoadState.Loading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                        is LoadState.Error -> Text(
                            (todos.loadState.refresh as LoadState.Error).error.message ?: "Error"
                        )
                        else -> LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = values,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                count = todos.itemCount,
                                key = todos.itemKey { it.id }
                            ) { index ->
                                val todo = todos[index] ?: return@items
                                TodoItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                        .clickable {
                                            idClicked = todo.id
                                            viewModel.onAction(TodoAction.ShowAddUpdateDialog(todo))
                                        },
                                    todo = todo,
                                    onCheckChanged = {
                                        viewModel.onAction(TodoAction.UpdateTodoCheck(todo))
                                    },
                                    onDelete = { viewModel.onAction(TodoAction.DeleteTodo(todo.id)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
