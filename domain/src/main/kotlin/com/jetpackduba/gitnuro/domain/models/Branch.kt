package com.jetpackduba.gitnuro.domain.models

import com.jetpackduba.gitnuro.domain.extensions.LOCAL_PREFIX_LENGTH
import com.jetpackduba.gitnuro.domain.extensions.REMOTE_PREFIX_LENGTH
import com.jetpackduba.gitnuro.domain.isLocal
import org.eclipse.jgit.lib.Constants

data class Branch(
    val hash: String,
    val name: String,
    val isLocal: Boolean,
) {
    val isRemote = !isLocal
    val simpleName: String
        get() {
            return when {
                this.name == Constants.HEAD -> {
                    this.name
                }

                this.isRemote -> {
                    val split = name.split("/")
                    split.takeLast(split.size - REMOTE_PREFIX_LENGTH).joinToString("/")
                }

                else -> {
                    val split = this.name.split("/")
                    split.takeLast(split.size - LOCAL_PREFIX_LENGTH).joinToString("/")
                }
            }
        }

    val remoteName: String
        get() {
            if (this.isLocal) {
                throw Exception("Trying to get remote name from a local branch")
            }
            val remoteWithoutPrefix = name.replace("refs/remotes/", "")
            val remoteName = remoteWithoutPrefix.split("/").firstOrNull()

            if (remoteName == null)
                throw Exception("Invalid remote name")
            else
                return remoteName
        }

    // TODO Override equals?
    fun isSameBranch(otherRef: Branch?): Boolean {
        if (this.name == Constants.HEAD && otherRef == null)
            return true

        if (otherRef == null)
            return false

        return this.name == otherRef.name
    }

}