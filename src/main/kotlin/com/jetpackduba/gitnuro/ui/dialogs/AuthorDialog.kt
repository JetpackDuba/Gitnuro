package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetpackduba.gitnuro.theme.outlinedTextFieldColors

import com.jetpackduba.gitnuro.theme.textButtonColors
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.viewmodels.AuthorViewModel

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

    MaterialDialog(
        onCloseRequested = onClose,
        background = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp),
        ) {

            Text(
                text = "Global settings",
                color = MaterialTheme.colors.onBackground,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp),
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
                color = MaterialTheme.colors.onBackground,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
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

            val visible = name.isNotBlank() || email.isNotBlank()

            val visibilityAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(visibilityAlpha)
            ) {
                Icon(
                    painterResource("warning.svg"),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Repository-level values will override global values",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 8.dp, start = 4.dp),
                )
            }
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
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colors.onBackground,
                        style = MaterialTheme.typography.body1,
                    )
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
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .width(80.dp)
                .padding(end = 16.dp),
        )

        AdjustableOutlinedTextField(
            value = value,
            modifier = Modifier
                .weight(1f),
            enabled = enabled,
            onValueChange = onValueChange,
            colors = outlinedTextFieldColors(),
            singleLine = true,
        )
    }
}