package com.jetpackduba.gitnuro.data.mappers

import com.jetpackduba.gitnuro.domain.models.Tag
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class JGitTagMapper @Inject constructor(): DataMapper<Tag?, Ref?> {
    override fun toData(value: Tag?): Nothing {
        throw NotImplementedError()
    }

    override fun toDomain(value: Ref?): Tag? {
        val value = value ?: return null

        return Tag(
            hash = value.objectId.name,
            name = value.name,
        )
    }
}