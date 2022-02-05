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
    isExpanded: MutableState<Boolean> = remember { mutableStateOf(true) },
    header: @Composable () -> Unit,
    child: @Composable () -> Unit,
) {
    VerticalExpandable(
        isExpanded = isExpanded.value,
        onExpand = { isExpanded.value = !isExpanded.value },
        header = header,
        child = child,
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VerticalExpandable(
    isExpanded: Boolean,
    onExpand: () -> Unit,
    header: @Composable () -> Unit,
    child: @Composable () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier.clickable {
                onExpand()
            }
        ) {
            header()
        }

        if(isExpanded) {
            child()
        }
    }
}