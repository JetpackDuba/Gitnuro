package com.jetpackduba.gitnuro.domain.models

import org.eclipse.jgit.diff.RawText

sealed interface EntryContent {
    data object Missing : EntryContent
    data object InvalidObjectBlob : EntryContent
    data class Text(val rawText: RawText) : EntryContent
    data object Submodule : EntryContent
    sealed interface BinaryContent : EntryContent
    data class ImageBinary(val imagePath: String, val contentType: String) : BinaryContent
    data object Binary : BinaryContent
    data object TooLargeEntry : EntryContent
}