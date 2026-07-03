package com.jetpackduba.gitnuro.data.mappers

import com.jetpackduba.gitnuro.domain.models.Submodule
import com.jetpackduba.gitnuro.domain.models.SubmoduleState
import org.eclipse.jgit.submodule.SubmoduleStatus
import org.eclipse.jgit.submodule.SubmoduleStatusType
import javax.inject.Inject

class JGitSubmoduleMapper @Inject constructor() : DataMapper<Submodule, SubmoduleStatus> {
    override fun toData(value: Submodule): Nothing {
        throw NotImplementedError("Mapping of Submodule domain to data not implemented")
    }

    override fun toDomain(value: SubmoduleStatus): Submodule {
        with (value) {
            return Submodule(
                indexId = indexId.name,
                path = path,
                state = when (type) {
                    SubmoduleStatusType.MISSING -> SubmoduleState.MISSING
                    SubmoduleStatusType.UNINITIALIZED -> SubmoduleState.UNINITIALIZED
                    SubmoduleStatusType.INITIALIZED -> SubmoduleState.INITIALIZED
                    SubmoduleStatusType.REV_CHECKED_OUT -> SubmoduleState.REV_CHECKED_OUT
                }
            )

        }
    }
}