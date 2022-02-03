package app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VerticalExpandable(
    header: @Composable () -> Unit,
    child: @Composable () -> Unit,
) {
    var isExpanded by remember {
        mutableStateOf(true)
    }
    Column {
        Box(
            modifier = Modifier.clickable {
                isExpanded = !isExpanded
            }
        ) {
            header()
        }

        AnimatedVisibility(visible = isExpanded) {
            child()
        }

    }

}