package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.apply_stash
import com.jetpackduba.gitnuro.generated.resources.delete
import org.jetbrains.compose.resources.painterResource

fun stashesContextMenuItems(
    onApply: () -> Unit,
    onPop: () -> Unit,
    onDelete: () -> Unit,
): List<ContextMenuElement> {
    return listOf(
        ContextMenuElement.ContextTextEntry(
            label = "Apply stash",
            icon = { painterResource(Res.drawable.apply_stash) },
            onClick = onApply
        ),
        ContextMenuElement.ContextTextEntry(
            label = "Pop stash",
            icon = { painterResource(Res.drawable.apply_stash) },
            onClick = onPop
        ),
        ContextMenuElement.ContextTextEntry(
            label = "Drop stash",
            icon = { painterResource(Res.drawable.delete) },
            onClick = onDelete
        ),
    )
}