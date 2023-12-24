package com.jetpackduba.gitnuro.git

import com.jetpackduba.gitnuro.ui.SelectedItem

sealed interface TaskEvent {
    data class ScrollToGraphItem(val selectedItem: SelectedItem) : TaskEvent
}