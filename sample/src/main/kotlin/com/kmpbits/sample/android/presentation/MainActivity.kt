package com.kmpbits.sample.android.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmpbits.netflow_core.states.ResultState
import com.kmpbits.sample.android.domain.model.Todo
import com.kmpbits.sample.android.presentation.theme.MyApplicationTheme
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel = koinViewModel<MainViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when(state) {
                        ResultState.Empty -> {}
                        is ResultState.Error -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text((state as ResultState.Error<List<Todo>>).error.errorBody.orEmpty())
                        }
                        is ResultState.Loading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                        is ResultState.Success -> {
                            val todos = (state as ResultState.Success<List<Todo>>).data

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                items(todos) { todo ->
                                    Text(text = todo.title)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}