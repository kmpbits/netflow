package com.kmpbits.sample.android.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun AddUpdateTodoDialog(
    onDismiss: () -> Unit,
    dialogTitle: String,
    todoTitle: String,
    buttonTitle: String,
    onAddToDo: () -> Unit,
    onTodoUpdate: (String) -> Unit,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier.width(300.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                TextField(
                    value = todoTitle,
                    onValueChange = {
                        onTodoUpdate(it)
                    },
                    placeholder = {
                        Text(text = "Enter title")
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Checked?")
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = onCheckedChange
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    TextButton(
                        onClick = onAddToDo
                    ) {
                        Text(buttonTitle)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AddUpdateTodoDialogPreview() {
    MaterialTheme {
        AddUpdateTodoDialog(
            onDismiss = {},
            dialogTitle = "Add Todo",
            todoTitle = "",
            buttonTitle = "Add",
            onAddToDo = {},
            onTodoUpdate = {},
            isChecked = false,
            onCheckedChange = {}
        )
    }

}