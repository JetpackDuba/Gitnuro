package com.jetpackduba.gitnuro.ui.tree_files

import kotlin.math.max

fun <T> entriesToTreeEntry(
    entries: List<T>,
    treeContractedDirs: List<String>,
    onGetEntryPath: (T) -> String,
): List<TreeItem<T>> {
    return entries
        .asSequence()
        .sortedWith { entry1, entry2 ->
            val path1 = onGetEntryPath(entry1)
            val path2 = onGetEntryPath(entry2)

            PathsComparator().compare(path1, path2)
        }
        .map { entry ->
            val filePath = onGetEntryPath(entry)
            val parts = filePath.split("/")

            parts.mapIndexed { index, partName ->
                if (index == parts.lastIndex) {
                    val isParentDirectoryContracted = treeContractedDirs.any { contractedDir ->
                        filePath.startsWith(contractedDir + "/")
                    }

                    if (isParentDirectoryContracted) {
                        null
                    } else {
                        TreeItem.File(entry, partName, filePath, index)
                    }
                } else {
                    val dirPath = parts.slice(0..index).joinToString("/")
                    val isParentDirectoryContracted = treeContractedDirs.any { contractedDir ->
                        dirPath.startsWith(contractedDir + "/") &&
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
        .toList()
}

private class PathsComparator : Comparator<String> {
    override fun compare(path1: String, path2: String): Int {
        val path1Parts = path1.split("/")
        val path2Parts = path2.split("/")
        
        val maxIndex = max(path1Parts.count(), path2Parts.count())

        for (i in 0 until maxIndex) {
            val part1 = path1Parts.getOrNull(i) ?: return -1
            val part2 = path2Parts.getOrNull(i) ?: return 1

            val comparison = part1.compareTo(part2)

            if (comparison != 0) {
                return comparison
            }
        }

        return 0
    }

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
