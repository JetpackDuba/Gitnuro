package app.extensions

import androidx.compose.foundation.lazy.LazyListState

fun LazyListState.observeScrollChanges() {
    // When accessing this property, this parent composable is recomposed when the scroll changes
    // because LazyListState is marked with @Stable
    this.firstVisibleItemScrollOffset
}