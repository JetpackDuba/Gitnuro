package app.ui.dialogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.mouseClickable
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.unit.dp
import app.git.ResetType
import app.theme.primaryTextColor
import app.ui.components.PrimaryButton

@Composable
fun ResetBranchDialog(
    onReject: () -> Unit,
    onAccept: (resetType: ResetType) -> Unit
) {
    var resetType by remember { mutableStateOf(ResetType.MIXED) }

    MaterialDialog {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Center,
        ) {
            RadioButtonText(
                selected = resetType == ResetType.SOFT,
                onClick = {
                    resetType = ResetType.SOFT
                },
                text = "Soft reset"
            )
            RadioButtonText(
                selected = resetType == ResetType.MIXED,
                onClick = {
                    resetType = ResetType.MIXED
                },
                text = "Mixed reset"
            )
            RadioButtonText(
                selected = resetType == ResetType.HARD,
                onClick = {
                    resetType = ResetType.HARD
                },
                text = "Hard reset"
            )
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.End)
            ) {
                TextButton(
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = {
                        onReject()
                    }
                ) {
                    Text("Cancel")
                }
                PrimaryButton(
                    onClick = {
                        onAccept(resetType)
                    },
                    text = "Reset branch"
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RadioButtonText(
    selected: Boolean,
    text: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: RadioButtonColors = RadioButtonDefaults.colors()
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .mouseClickable {
                if (this.buttons.isPrimaryPressed) {
                    if (onClick != null) {
                        onClick()
                    }
                }
            }
    ) {
        RadioButton(
            selected,
            onClick,
            modifier,
            enabled,
            interactionSource,
            colors
        )

        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colors.primaryTextColor,
        )
    }
}