package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.person
import com.jetpackduba.gitnuro.models.AuthorInfo
import com.jetpackduba.gitnuro.theme.outlinedTextFieldColors
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.dialogs.base.MaterialDialog
import org.jetbrains.compose.resources.painterResource

@Composable
fun CommitAuthorDialog(
    authorInfo: AuthorInfo,
    onClose: () -> Unit,
    onAccept: (newAuthorInfo: AuthorInfo, persist: Boolean) -> Unit,
) {
    var globalName by remember(authorInfo) { mutableStateOf(authorInfo.globalName.orEmpty()) }
    var globalEmail by remember(authorInfo) { mutableStateOf(authorInfo.globalEmail.orEmpty()) }
    var persist by remember { mutableStateOf(false) }

    MaterialDialog(
        onCloseRequested = onClose,
        background = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .width(IntrinsicSize.Min),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Icon(
                painterResource(Res.drawable.person),
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(64.dp),
                tint = MaterialTheme.colors.onBackground,
            )


            Text(
                text = "Author identity",
                modifier = Modifier
                    .padding(bottom = 8.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = "Your Git configuration does not have a user identity set.",
                modifier = Modifier,
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )

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

            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.End)
            ) {
                PrimaryButton(
                    text = "Cancel",
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = onClose,
                    backgroundColor = Color.Transparent,
                    textColor = MaterialTheme.colors.onBackground,
                )
                PrimaryButton(
                    onClick = {
                        onAccept(
                            AuthorInfo(
                                globalName,
                                globalEmail,
                                authorInfo.name,
                                authorInfo.email,
                            ),
                            persist,
                        )
                    },
                    text = "Continue"
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
