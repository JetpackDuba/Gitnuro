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
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.reset_branch_dialog_action
import com.jetpackduba.gitnuro.app.generated.resources.reset_branch_dialog_entry_hard_subtitle
import com.jetpackduba.gitnuro.app.generated.resources.reset_branch_dialog_entry_hard_title
import com.jetpackduba.gitnuro.app.generated.resources.reset_branch_dialog_entry_mixed_subtitle
import com.jetpackduba.gitnuro.app.generated.resources.reset_branch_dialog_entry_mixed_title
import com.jetpackduba.gitnuro.app.generated.resources.reset_branch_dialog_entry_soft_subtitle
import com.jetpackduba.gitnuro.app.generated.resources.reset_branch_dialog_entry_soft_title
import com.jetpackduba.gitnuro.app.generated.resources.reset_branch_dialog_subtitle
import com.jetpackduba.gitnuro.app.generated.resources.reset_branch_dialog_title
import com.jetpackduba.gitnuro.app.generated.resources.undo
import com.jetpackduba.gitnuro.domain.usecases.ResetType
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.dialogs.base.IconBasedDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ResetBranchDialog(
    viewModel: ResetBranchViewModel,
    onDismiss: () -> Unit,
) {
    var resetType by remember { mutableStateOf(ResetType.MIXED) }

    IconBasedDialog(
        icon = painterResource(Res.drawable.undo),
        title = stringResource(Res.string.reset_branch_dialog_title),
        subtitle = stringResource(Res.string.reset_branch_dialog_subtitle),
        primaryActionText = stringResource(Res.string.reset_branch_dialog_action),
        onDismiss = onDismiss,
        onPrimaryActionClicked = {
            viewModel.reset(resetType)
            onDismiss()
        },
        beforeActionsFocusRequester = null,
        actionsFocusRequester = null,
        afterActionsFocusRequester = null,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButtonText(
                isSelected = resetType == ResetType.SOFT,
                title = stringResource(Res.string.reset_branch_dialog_entry_soft_title),
                subtitle = stringResource(Res.string.reset_branch_dialog_entry_soft_subtitle),
                onClick = {
                    resetType = ResetType.SOFT
                },
            )
            RadioButtonText(
                isSelected = resetType == ResetType.MIXED,
                title = stringResource(Res.string.reset_branch_dialog_entry_mixed_title),
                subtitle = stringResource(Res.string.reset_branch_dialog_entry_mixed_subtitle),
                onClick = {
                    resetType = ResetType.MIXED
                },
            )
            RadioButtonText(
                isSelected = resetType == ResetType.HARD,
                title = stringResource(Res.string.reset_branch_dialog_entry_hard_title),
                subtitle = stringResource(Res.string.reset_branch_dialog_entry_hard_subtitle),
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