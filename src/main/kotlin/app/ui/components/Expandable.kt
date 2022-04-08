package app.ui.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

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

        if (isExpanded) {
            child()
        }
    }
}