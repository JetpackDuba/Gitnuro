package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.undo
import com.jetpackduba.gitnuro.git.log.ResetType
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.dialogs.base.IconBasedDialog
import org.jetbrains.compose.resources.painterResource

@Composable
fun ResetBranchDialog(
    onDismiss: () -> Unit,
    onAccept: (resetType: ResetType) -> Unit,
) {
    var resetType by remember { mutableStateOf(ResetType.MIXED) }

    IconBasedDialog(
        icon = painterResource(Res.drawable.undo),
        title = "Reset current branch",
        subtitle = "Reset the changes of your current branch to a \nprevious or different commit",
        primaryActionText = "Reset branch",
        onDismiss = onDismiss,
        onPrimaryActionClicked = { onAccept(resetType) },
        beforeActionsFocusRequester = null,
        actionsFocusRequester = null,
        afterActionsFocusRequester = null,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButtonText(
                isSelected = resetType == ResetType.SOFT,
                title = "Soft reset",
                subtitle = "Keep the changes in the index (staged)",
                onClick = {
                    resetType = ResetType.SOFT
                },
            )
            RadioButtonText(
                isSelected = resetType == ResetType.MIXED,
                title = "Mixed reset",
                subtitle = "Keep the changes (unstaged)",
                onClick = {
                    resetType = ResetType.MIXED
                },
            )
            RadioButtonText(
                isSelected = resetType == ResetType.HARD,
                title = "Hard",
                subtitle = "Discard all the changes",
                onClick = {
                    resetType = ResetType.HARD
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RadioButtonText(
    isSelected: Boolean,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    val color = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.surface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(380.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colors.background)
            .border(2.dp, color, RoundedCornerShape(8.dp))
            .onClick {
                if (onClick != null) {
                    onClick()
                }
            }
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .clip(CircleShape)
                .background(color)
                .size(16.dp)
        )

        Column(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1,
            )

            Text(
                text = subtitle,
                color = MaterialTheme.colors.onBackgroundSecondary,
                style = MaterialTheme.typography.body2,
            )
        }
    }
}