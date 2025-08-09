package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.push_context_menu_force_push
import com.jetpackduba.gitnuro.generated.resources.push_context_menu_push_including_tags
import com.jetpackduba.gitnuro.generated.resources.tag
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun pushContextMenuItems(
    onPushWithTags: () -> Unit,
    onForcePush: () -> Unit,
): List<ContextMenuElement> {
    return mutableListOf(
        ContextMenuElement.ContextTextEntry(
            composableLabel = { stringResource(Res.string.push_context_menu_push_including_tags) },
            icon = { painterResource(Res.drawable.tag) },
            onClick = onPushWithTags,
        ),
        ContextMenuElement.ContextTextEntry(
            composableLabel = { stringResource(Res.string.push_context_menu_force_push) },
            onClick = onForcePush,
        ),
    )
}
