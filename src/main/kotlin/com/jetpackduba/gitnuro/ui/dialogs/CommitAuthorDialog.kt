package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.generic_button_continue
import com.jetpackduba.gitnuro.generated.resources.person
import com.jetpackduba.gitnuro.models.AuthorInfo
import com.jetpackduba.gitnuro.theme.outlinedTextFieldColors
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.dialogs.base.IconBasedDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CommitAuthorDialog(
    authorInfo: AuthorInfo,
    onClose: () -> Unit,
    onAccept: (newAuthorInfo: AuthorInfo, persist: Boolean) -> Unit,
) {
    var globalName by remember(authorInfo) { mutableStateOf(authorInfo.globalName.orEmpty()) }
    var globalEmail by remember(authorInfo) { mutableStateOf(authorInfo.globalEmail.orEmpty()) }
    var persist by remember { mutableStateOf(false) }

    IconBasedDialog(
        icon = painterResource(Res.drawable.person),
        title = "Author identity",
        subtitle = "Your Git configuration does not have a user identity set.",
        isPrimaryActionEnabled = true,
        primaryActionText = stringResource(Res.string.generic_button_continue),
        beforeActionsFocusRequester = null,
        actionsFocusRequester = null,
        afterActionsFocusRequester = null,
        onDismiss = onClose,
        onPrimaryActionClicked = { onAccept(AuthorInfo(globalName, globalEmail, null, null), persist) },
    ) {
        Text(
            text = "What identity would you like to use for this commit?",
            modifier = Modifier
                .padding(bottom = 8.dp),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.handMouseClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                persist = !persist
            }.fillMaxWidth()
        ) {
            Checkbox(
                checked = persist,
                onCheckedChange = {
                    persist = it
                },
                modifier = Modifier
                    .padding(all = 8.dp)
                    .size(12.dp)
            )

            Text(
                "Save identity in the .gitconfig file",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
            )
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
    Column(
        modifier = Modifier
            .width(400.dp)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(bottom = 8.dp),
        )

        AdjustableOutlinedTextField(
            value = value,
            modifier = Modifier
                .fillMaxWidth(),
            enabled = enabled,
            onValueChange = onValueChange,
            colors = outlinedTextFieldColors(),
            singleLine = true,
        )
    }
}
