package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.pull_context_menu_fetch_all
import com.jetpackduba.gitnuro.generated.resources.pull_context_menu_pull_with_merge
import com.jetpackduba.gitnuro.generated.resources.pull_context_menu_pull_with_rebase
import org.jetbrains.compose.resources.stringResource

fun pullContextMenuItems(
    onPullWith: () -> Unit,
    onFetchAll: () -> Unit,
    isPullWithRebaseDefault: Boolean,
): List<ContextMenuElement> {
    return mutableListOf(
        ContextMenuElement.ContextTextEntry(
            composableLabel = {
                if (isPullWithRebaseDefault) {
                    stringResource(Res.string.pull_context_menu_pull_with_merge)
                } else {
                    stringResource(Res.string.pull_context_menu_pull_with_rebase)
                }
            },
            onClick = onPullWith,
        ),
        ContextMenuElement.ContextTextEntry(
            composableLabel = { stringResource(Res.string.pull_context_menu_fetch_all) },
            onClick = onFetchAll,
        ),
    )
}
