package com.jetpackduba.gitnuro.ui.tree_files

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TreeTest {

    @Test
    fun `test entriesToTreeEntry with empty entries`() {
        val entries = emptyList<String>()
        val treeContractedDirs = emptyList<String>()
        val result = entriesToTreeEntry(true, entries, treeContractedDirs) { it }
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test entriesToTreeEntry with single file entry`() {
        val entries = listOf("file.txt")
        val treeContractedDirs = emptyList<String>()
        val result = entriesToTreeEntry(true, entries, treeContractedDirs) { it }

        assertEquals(listOf(TreeItem.File("file.txt", "file.txt", "file.txt", 0)), result)
    }

    @Test
    fun `test entriesToTreeEntry with multiple file entries`() {
        val entries = listOf(
            "dir1/file1.txt",
            "dir2/file2.txt",
            "dir3/file3.txt",
        )
        val treeContractedDirs = emptyList<String>()
        val result = entriesToTreeEntry(true, entries, treeContractedDirs) { it }
        val expected = listOf(
            TreeItem.Dir(true, "dir1", "dir1", 0),
            TreeItem.File("dir1/file1.txt", "file1.txt", "dir1/file1.txt", 1),
            TreeItem.Dir(true, "dir2", "dir2", 0),
            TreeItem.File("dir2/file2.txt", "file2.txt", "dir2/file2.txt", 1),
            TreeItem.Dir(true, "dir3", "dir3", 0),
            TreeItem.File("dir3/file3.txt", "file3.txt", "dir3/file3.txt", 1)
        )
        assertEquals(expected, result)
    }

    @Test
    fun `test entriesToTreeEntry with similar names`() {
        val entries = listOf(
            "webpack/webpack.config2.ts",
            "webpack/webpack.config.ts",
            "webpack-plugin.ts",
            "dir1/file3.txt"
        )
        val treeContractedDirs = emptyList<String>()
        val result = entriesToTreeEntry(true, entries, treeContractedDirs) { it }
        val expected = listOf(
            TreeItem.Dir(true, "dir1", "dir1", 0),
            TreeItem.File("dir1/file3.txt", "file3.txt", "dir1/file3.txt", 1),
            TreeItem.Dir(true, "webpack", "webpack", 0),
            TreeItem.File("webpack/webpack.config.ts", "webpack.config.ts", "webpack/webpack.config.ts", 1),
            TreeItem.File("webpack/webpack.config2.ts", "webpack.config2.ts", "webpack/webpack.config2.ts", 1),
            TreeItem.File("webpack-plugin.ts", "webpack-plugin.ts", "webpack-plugin.ts", 0)
        )
        assertEquals(expected, result)
    }

    @Test
    fun `test test entriesToTreeEntry with similar names with contracted directories`() {
        val entries = listOf(
            "webpack/webpack.config2.ts",
            "webpack/webpack.config.ts",
            "webpack-plugin.ts",
            "dir1/file3.txt"
        )
        val treeContractedDirs = listOf<String>("webpack", "dir1")
        val result = entriesToTreeEntry(true, entries, treeContractedDirs) { it }
        val expected = listOf(
            TreeItem.Dir(false, "dir1", "dir1", 0),
            TreeItem.Dir(false, "webpack", "webpack", 0),
            TreeItem.File("webpack-plugin.ts", "webpack-plugin.ts", "webpack-plugin.ts", 0)
        )
        assertEquals(expected, result)
    }
}