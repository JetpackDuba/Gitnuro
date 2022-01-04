@file:OptIn(ExperimentalComposeUiApi::class)

package app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
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
import app.viewmodels.MenuViewModel

@Composable
fun Menu(
    menuViewModel: MenuViewModel,
    onRepositoryOpen: () -> Unit,
    onCreateBranch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        MenuButton(
            modifier = Modifier.padding(start = 8.dp),
            title = "Open",
            icon = painterResource("open.svg"),
            onClick = {
                onRepositoryOpen()
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        MenuButton(
            title = "Pull",
            icon = painterResource("download.svg"),
            onClick = { menuViewModel.pull() },
        )

        MenuButton(
            title = "Push",
            icon = painterResource("upload.svg"),
            onClick = { menuViewModel.push() },
        )

        Spacer(modifier = Modifier.width(16.dp))

        MenuButton(
            title = "Branch",
            icon = painterResource("branch.svg"),
            onClick = {
                onCreateBranch()
            },
        )


        Spacer(modifier = Modifier.width(16.dp))

        MenuButton(
            title = "Stash",
            icon = painterResource("stash.svg"),
            onClick = { menuViewModel.stash() },
        )
        MenuButton(
            title = "Pop",
            icon = painterResource("apply_stash.svg"),
            onClick = { menuViewModel.popStash() },
        )

        Spacer(modifier = Modifier.weight(1f))
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

    OutlinedButton(
        modifier = modifier
            .padding(horizontal = 2.dp),
        enabled = enabled,
        onClick = onClick,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = icon,
                contentDescription = title,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
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

