package com.jetpackduba.gitnuro.app

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object TestUtils {
    fun copyDir(src: String, dest: String) {
        try {
            Files.walk(Paths.get(src)).forEach { pathA: Path ->
                val pathB: Path = Paths.get(dest, pathA.toString().substring(src.length))
                try {
                    if (pathA.toString() != src) Files.copy(
                        pathA,
                        pathB,
                    )
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}