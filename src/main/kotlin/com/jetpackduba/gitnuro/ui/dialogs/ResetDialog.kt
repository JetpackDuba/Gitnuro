package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.git.log.ResetType
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.components.PrimaryButton

@Composable
fun ResetBranchDialog(
    onReject: () -> Unit,
    onAccept: (resetType: ResetType) -> Unit
) {
    var resetType by remember { mutableStateOf(ResetType.MIXED) }

    MaterialDialog(onCloseRequested = onReject) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painterResource(AppIcons.UNDO),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .padding(vertical = 16.dp),
                tint = MaterialTheme.colors.onBackground,
            )

            Text(
                text = "Reset current branch",
                modifier = Modifier
                    .padding(bottom = 8.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1,
            )

            Text(
                text = "Reset the changes of your current branch to a \nprevious or different commit",
                modifier = Modifier
                    .padding(bottom = 16.dp),
                color = MaterialTheme.colors.onBackgroundSecondary,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )

            Column(
//                modifier = Modifier
//                    .padding(horizontal = 16.dp),
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
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.End)
            ) {
                PrimaryButton(
                    text = "Cancel",
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = onReject,
                    backgroundColor = Color.Transparent,
                    textColor = MaterialTheme.colors.onBackground,
                )
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