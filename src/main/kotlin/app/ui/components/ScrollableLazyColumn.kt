package app.ui.components

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.theme.scrollbarHover
import app.theme.scrollbarNormal

@Composable
fun ScrollableLazyColumn(
    modifier: Modifier,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit
) {
    Box(
        modifier = modifier,
    ) {
        LazyColumn(
            state = state,
            content = content
        )
        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 4.dp),
            style = LocalScrollbarStyle.current.copy(
                unhoverColor = MaterialTheme.colors.scrollbarNormal,
                hoverColor = MaterialTheme.colors.scrollbarHover,
            ),
            adapter = rememberScrollbarAdapter(
                scrollState = state
            )
        )
    }
}