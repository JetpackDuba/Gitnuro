package com.jetpackduba.gitnuro.data.repositories.configuration.mappers

import com.jetpackduba.gitnuro.data.mappers.DataMapper
import com.jetpackduba.gitnuro.domain.models.DiffTextViewType
import javax.inject.Inject

private const val SPLIT = "split"
private const val UNIFIED = "unified"

class TextDiffViewTypeMapper @Inject constructor(): DataMapper<DiffTextViewType?, String?>  {
    override fun toData(value: DiffTextViewType?): String? {
        return when (value) {
            DiffTextViewType.Split -> SPLIT
            DiffTextViewType.Unified -> UNIFIED
            null -> null
        }
    }

    override fun toDomain(value: String?): DiffTextViewType? {
        return when (value) {
            SPLIT -> DiffTextViewType.Split
            UNIFIED -> DiffTextViewType.Unified
            null -> null
            else -> throw IllegalStateException("Unhandled text diff view $value")
        }
    }
}