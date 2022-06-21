package app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.mouseClickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theme.outlinedTextFieldColors
import app.theme.primaryTextColor
import app.theme.textButtonColors
import app.ui.components.PrimaryButton
import app.viewmodels.AuthorViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthorDialog(
    authorViewModel: AuthorViewModel,
    onClose: () -> Unit,
) {
    val authorInfo by authorViewModel.authorInfo.collectAsState()

    var globalName by remember(authorInfo) { mutableStateOf(authorInfo.globalName.orEmpty()) }
    var globalEmail by remember(authorInfo) { mutableStateOf(authorInfo.globalEmail.orEmpty()) }
    var name by remember(authorInfo) { mutableStateOf(authorInfo.name.orEmpty()) }
    var email by remember(authorInfo) { mutableStateOf(authorInfo.email.orEmpty()) }

    MaterialDialog(onCloseRequested = onClose) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.background)
                .padding(horizontal = 8.dp),
        ) {

            Text(
                text = "Global settings",
                color = MaterialTheme.colors.primaryTextColor,
                fontSize = 18.sp,
            )

            TextInput(
                title = "Name",
                value = globalName,
                onValueChange = { globalName = it },
            )
            TextInput(
                title = "Email",
                value = globalEmail,
                onValueChange = { globalEmail = it },
            )

            Text(
                text = "Repository settings",
                color = MaterialTheme.colors.primaryTextColor,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp),
            )

            TextInput(
                title = "Name",
                value = name,
                onValueChange = { name = it },
            )
            TextInput(
                title = "Email",
                value = email,
                onValueChange = { email = it },
            )

            Text(
                text = "Repository-level values will override global values",
                color = MaterialTheme.colors.primaryTextColor,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.End)
            ) {
                TextButton(
                    modifier = Modifier.padding(end = 8.dp),
                    colors = textButtonColors(),
                    onClick = {
                        onClose()
                    }
                ) {
                    Text("Cancel")
                }
                PrimaryButton(
                    onClick = {
                        authorViewModel.saveAuthorInfo(
                            globalName,
                            globalEmail,
                            name,
                            email,
                        )
                        onClose()
                    },
                    text = "Save"
                )
            }
        }
    }
}


@Composable
private fun TextInput(
    title: String,
    value: String,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .width(400.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = MaterialTheme.colors.primaryTextColor,
            fontSize = 16.sp,
            modifier = Modifier
                .width(100.dp)
                .padding(end = 16.dp),
        )


        OutlinedTextField(
            value = value,
            modifier = Modifier
                .weight(1f),
            enabled = enabled,
            onValueChange = onValueChange,
            colors = outlinedTextFieldColors(),
            maxLines = 1,
        )
    }
}