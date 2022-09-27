package com.jetpackduba.gitnuro.extensions

import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectIdRef
import org.eclipse.jgit.lib.Ref

// Remotes can have slashes in the name, but we won't care about it, known issue
private const val REMOTE_PREFIX_LENGTH = 3
private const val LOCAL_PREFIX_LENGTH = 2

val Ref.simpleName: String
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

val Ref.simpleLogName: String
    get() {
        return when {
            this.name == Constants.HEAD -> {
                this.name
            }
            this.isRemote -> {
                name.replace("refs/remotes/", "")
            }
            else -> {
                val split = this.name.split("/")
                split.takeLast(split.size - LOCAL_PREFIX_LENGTH).joinToString("/")
            }
        }
    }

val Ref.remoteName: String
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

val Ref.isBranch: Boolean
    get() {
        return this is ObjectIdRef.PeeledNonTag
    }

val Ref.isHead: Boolean
    get() {
        return this.name == Constants.HEAD
    }

val Ref.isTag: Boolean
    get() = this.name.startsWith(Constants.R_TAGS)

val Ref.isLocal: Boolean
    get() = !this.isRemote

val Ref.isRemote: Boolean
    get() = this.name.startsWith(Constants.R_REMOTES)

fun Ref.isSameBranch(otherRef: Ref?): Boolean {
    if (this.name == Constants.HEAD && otherRef == null)
        return true

    if (otherRef == null)
        return false

    return this.name == otherRef.name
}
