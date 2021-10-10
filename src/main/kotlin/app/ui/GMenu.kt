package app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerMoveFilter
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
) {
    val openHovering = remember { mutableStateOf(false) }
    val pullHovering = remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        MenuButton(
            title = "Open",
            hovering = openHovering,
            icon = painterResource("open.svg"),
            onClick = {
                openHovering.value = false // Without this, the hover would be kept because of the newly opened dialog
                onRepositoryOpen()
            }
        )
        MenuButton(
            title = "Pull",
            hovering = pullHovering,
            icon = painterResource("download.svg"),
            onClick = {
                pullHovering.value = false
                onPull()
            },
        )
        MenuButton(
            title = "Push",
            icon = painterResource("upload.svg"),
            onClick = onPush,
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
    hovering: MutableState<Boolean> = remember { mutableStateOf(false) },
    title: String,
    icon: Painter,
    onClick: () -> Unit
) {
    val backgroundColor = if (hovering.value)
        MaterialTheme.colors.primary.copy(alpha = 0.15F)
    else
        Color.Transparent

    val iconColor = if (enabled) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.secondaryVariant
    }

    TextButton(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .pointerMoveFilter(
                onEnter = {
                    hovering.value = true
                    false
                },
                onExit = {
                    hovering.value = false
                    false
                }
            ),
        enabled = enabled,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            disabledBackgroundColor = Color.Transparent
        )
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
