package com.jetpackduba.gitnuro.data.git

import org.eclipse.jgit.api.Git
import java.io.File

// TODO instead of this, consider creating a pool of (J)Git instances that can be used by different actions of
//  different tabs or just some state holder with it
suspend fun <T> jgit(path: String, block: suspend Git.() -> T): T {
    return Git
        .open(File(path))
        .block()
}