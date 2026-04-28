package com.jetpackduba.gitnuro.data.mappers

import com.jetpackduba.gitnuro.data.extensions.isLocal
import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class JGitBranchMapper @Inject constructor(): DataMapper<Branch?, Ref?> {
    override fun toData(value: Branch?): Nothing {
        throw NotImplementedError()
    }

    override fun toDomain(value: Ref?): Branch? {
        val value = value ?: return null

        return Branch(
            hash = value.objectId.name,
            name = value.name,
            isLocal = value.isLocal,
        )
    }
}