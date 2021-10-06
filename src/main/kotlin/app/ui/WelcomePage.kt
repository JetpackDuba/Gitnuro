package app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.git.GitManager
import openRepositoryDialog


@Composable
fun WelcomePage(gitManager: GitManager) {
    Row(
        modifier = Modifier
            .fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            ButtonTile(
                title = "Open repository",
                painter = painterResource("open.svg"),
                onClick = { openRepositoryDialog(gitManager) }
            )

            ButtonTile(
                title = "Clone repository",
                painter = painterResource("open.svg"),
                onClick = {}
            )
        }
    }
}

@Composable
fun ButtonTile(
    title: String,
    painter: Painter,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(width = 200.dp, height = 56.dp)
    ) {
        Image(
            modifier = Modifier
                .size(24.dp)
                .padding(end = 8.dp),
            painter = painter,
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
        )

        Text(
            text = title,
        )
    }
}