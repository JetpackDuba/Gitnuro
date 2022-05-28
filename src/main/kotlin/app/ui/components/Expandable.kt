package app.ui.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.extensions.handMouseClickable

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
            modifier = Modifier.handMouseClickable {
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