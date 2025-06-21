package com.kmpbits.sample.android.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.kmpbits.sample.android.domain.model.Todo

@Composable
fun TodoItem(
    modifier: Modifier = Modifier,
    todo: Todo,
    onCheckChanged: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = todo.title,
                style = MaterialTheme.typography.bodyLarge,
            )

            Checkbox(
                checked = todo.completed,
                onCheckedChange = onCheckChanged,
            )

            IconButton(
                onClick = onDelete,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Todo",
                    tint = Color.Red
                )
            }
        }

        HorizontalDivider()
    }
}

@Preview
@Composable
private fun TodoItemPreview() {
    MaterialTheme {
        TodoItem(
            todo = Todo(
                userId = 1,
                id = 1,
                title = "Todo 1",
                completed = true,
                addedTimestamp = System.currentTimeMillis()
            ),
            onCheckChanged = {},
            onDelete = {}
        )
    }
}