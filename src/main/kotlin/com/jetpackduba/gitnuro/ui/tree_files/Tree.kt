package com.jetpackduba.gitnuro.ui.tree_files

import com.jetpackduba.gitnuro.system.systemSeparator

fun <T> entriesToTreeEntry(
    entries: List<T>,
    treeContractedDirs: List<String>,
    onGetEntryPath: (T) -> String,
): List<TreeItem<T>> {
    return entries
        .asSequence()
        .map { entry ->
            val filePath = onGetEntryPath(entry)
            val parts = filePath.split(systemSeparator)

            parts.mapIndexed { index, partName ->
                if (index == parts.lastIndex) {
                    val isParentContracted = treeContractedDirs.none { contractedDir ->
                        filePath.startsWith(contractedDir + systemSeparator)
                    }

                    if (isParentContracted) {
                        TreeItem.File(entry, partName, filePath, index)
                    } else {
                        null
                    }
                } else {
                    val dirPath = parts.slice(0..index).joinToString(systemSeparator)
                    val isParentDirectoryContracted = treeContractedDirs.any { contractedDir ->
                        dirPath.startsWith(contractedDir + systemSeparator) &&
                                dirPath != contractedDir
                    }
                    val isExactDirectoryContracted = treeContractedDirs.any { contractedDir ->
                        dirPath == contractedDir
                    }

                    when {
                        isParentDirectoryContracted -> null
                        isExactDirectoryContracted -> TreeItem.Dir(false, partName, dirPath, index)
                        else -> TreeItem.Dir(true, partName, dirPath, index)
                    }
                }
            }
        }
        .flatten()
        .filterNotNull()
        .distinct()
        .sortedBy { it.fullPath }
        .toList()
}

sealed interface TreeItem<out T> {
    val fullPath: String
    val displayName: String
    val depth: Int

    data class Dir(
        val isExpanded: Boolean,
        override val displayName: String,
        override val fullPath: String,
        override val depth: Int
    ) : TreeItem<Nothing>

    data class File<T>(
        val data: T,
        override val displayName: String,
        override val fullPath: String,
        override val depth: Int
    ) : TreeItem<T>
}
