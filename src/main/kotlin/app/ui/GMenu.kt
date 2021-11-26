@file:OptIn(ExperimentalComposeUiApi::class)

package app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theme.primaryTextColor

@Composable
fun GMenu(
    onRepositoryOpen: () -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onStash: () -> Unit,
    onPopStash: () -> Unit,
    onCreateBranch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        MenuButton(
            title = "Open",
            icon = painterResource("open.svg"),
            onClick = {
                onRepositoryOpen()
            }
        )
        MenuButton(
            title = "Pull",
            icon = painterResource("download.svg"),
            onClick = {
                onPull()
            },
        )
        MenuButton(
            title = "Push",
            icon = painterResource("upload.svg"),
            onClick = {
                onPush()
            },
        )
        MenuButton(
            title = "Branch",
            icon = painterResource("branch.svg"),
            onClick = {
                onCreateBranch()
            },
        )
        MenuButton(
            title = "Stash",
            icon = painterResource("stash.svg"),
            onClick = onStash,
        )
        MenuButton(
            title = "Pop",
            icon = painterResource("apply_stash.svg"),
            onClick = onPopStash,
        )
    }
}

@Composable
fun MenuButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String,
    icon: Painter,
    onClick: () -> Unit
) {
    val iconColor = if (enabled) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.secondaryVariant
    }

    TextButton(
        modifier = modifier
            .padding(horizontal = 2.dp),
        enabled = enabled,
        onClick = onClick,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = icon,
                contentDescription = title,
                modifier = Modifier
                    .padding(4.dp)
                    .size(24.dp),
                colorFilter = ColorFilter.tint(iconColor),
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colors.primaryTextColor
            )
        }

    }
}
