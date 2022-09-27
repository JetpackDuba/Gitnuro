package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.theme.scrollbarHover
import com.jetpackduba.gitnuro.theme.scrollbarNormal

@Composable
fun ScrollableColumn(
    modifier: Modifier,
    state: ScrollState = rememberScrollState(0),
    content: @Composable ColumnScope.() -> Unit
) {

    Box(
        modifier = modifier,
    ) {
        Column(
            content = content,
            modifier = Modifier
                .verticalScroll(state)
        )

        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 2.dp),
            style = LocalScrollbarStyle.current.copy(
                unhoverColor = MaterialTheme.colors.scrollbarNormal,
                hoverColor = MaterialTheme.colors.scrollbarHover,
            ),
            adapter = rememberScrollbarAdapter(
                scrollState = state
            ),
        )
    }
}