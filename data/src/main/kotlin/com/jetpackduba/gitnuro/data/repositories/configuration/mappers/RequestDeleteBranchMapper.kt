package com.jetpackduba.gitnuro.data.repositories.configuration.mappers

import com.jetpackduba.gitnuro.data.mappers.DataMapper
import com.jetpackduba.gitnuro.domain.models.RequestDeleteBranch
import javax.inject.Inject

private const val ALWAYS = "always"
private const val UNMERGED = "unmerged"
private const val NEVER = "never"

class RequestDeleteBranchMapper @Inject constructor() : DataMapper<RequestDeleteBranch, String> {
    override fun toData(value: RequestDeleteBranch): String {
        return when (value) {
            RequestDeleteBranch.ALWAYS -> ALWAYS
            RequestDeleteBranch.UNMERGED -> UNMERGED
            RequestDeleteBranch.NEVER -> NEVER
        }
    }

    override fun toDomain(value: String): RequestDeleteBranch {
        return when (value) {
            ALWAYS -> RequestDeleteBranch.ALWAYS
            UNMERGED -> RequestDeleteBranch.UNMERGED
            NEVER -> RequestDeleteBranch.NEVER
            else -> throw NotImplementedError("Mapping of requestDelete branch is not implemented for $value")
        }
    }
}