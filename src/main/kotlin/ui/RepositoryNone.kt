package ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource


@Composable
fun WelcomePage() {
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
            Button(onClick = {}) {
                Image(
                    painter = painterResource("open.svg"),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(contentColorFor(MaterialTheme.colors.primary))

                )
                Text("Open repository")

            }
        }
    }
}


