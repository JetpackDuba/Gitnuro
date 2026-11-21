package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.author_dialog_email
import com.jetpackduba.gitnuro.app.generated.resources.author_dialog_global_settings
import com.jetpackduba.gitnuro.app.generated.resources.author_dialog_name
import com.jetpackduba.gitnuro.app.generated.resources.author_dialog_override_global_values
import com.jetpackduba.gitnuro.app.generated.resources.author_dialog_repository_settings
import com.jetpackduba.gitnuro.app.generated.resources.generic_button_cancel
import com.jetpackduba.gitnuro.app.generated.resources.generic_save_changes
import com.jetpackduba.gitnuro.app.generated.resources.warning
import com.jetpackduba.gitnuro.theme.outlinedTextFieldColors
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.dialogs.base.MaterialDialog
import com.jetpackduba.gitnuro.viewmodels.AuthorViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AuthorDialog(
    viewModel: AuthorViewModel,
    onDismiss: () -> Unit,
) {
    val authorInfo by viewModel.authorInfo.collectAsState()

    var globalName by remember(authorInfo) { mutableStateOf(authorInfo.globalIdentity.name) }
    var globalEmail by remember(authorInfo) { mutableStateOf(authorInfo.globalIdentity.email) }
    var name by remember(authorInfo) { mutableStateOf(authorInfo.repositoryIdentity.name) }
    var email by remember(authorInfo) { mutableStateOf(authorInfo.repositoryIdentity.email) }

    MaterialDialog(
        onCloseRequested = onDismiss,
        background = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp),
        ) {

            Text(
                text = stringResource(Res.string.author_dialog_global_settings),
                color = MaterialTheme.colors.onBackground,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            TextInput(
                title = stringResource(Res.string.author_dialog_name),
                value = globalName.orEmpty(),
                onValueChange = { globalName = it },
            )
            TextInput(
                title = stringResource(Res.string.author_dialog_email),
                value = globalEmail.orEmpty(),
                onValueChange = { globalEmail = it },
            )

            Text(
                text = stringResource(Res.string.author_dialog_repository_settings),
                color = MaterialTheme.colors.onBackground,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )

            TextInput(
                title = stringResource(Res.string.author_dialog_name),
                value = name.orEmpty(),
                onValueChange = { name = it },
            )
            TextInput(
                title = stringResource(Res.string.author_dialog_email),
                value = email.orEmpty(),
                onValueChange = { email = it },
            )

            val visible = !name.isNullOrBlank() || !email.isNullOrBlank()

            val visibilityAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(visibilityAlpha)
            ) {
                Icon(
                    painterResource(Res.drawable.warning),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(Res.string.author_dialog_override_global_values),
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
                PrimaryButton(
                    text = stringResource(Res.string.generic_button_cancel),
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = onDismiss,
                    backgroundColor = Color.Transparent,
                    textColor = MaterialTheme.colors.onBackground,
                )
                PrimaryButton(
                    onClick = {
                        viewModel.saveAuthorInfo(
                            globalName,
                            globalEmail,
                            name,
                            email,
                        )
                        onDismiss()
                    },
                    text = stringResource(Res.string.generic_save_changes),
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