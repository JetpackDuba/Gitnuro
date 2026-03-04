package com.jetpackduba.gitnuro.domain.git.diff

import com.jetpackduba.gitnuro.domain.git.EntryContent
import org.eclipse.jgit.diff.RawText
import javax.inject.Inject

class CanGenerateTextDiffGitAction @Inject constructor() {
    inline operator fun invoke(
        rawOld: EntryContent,
        rawNew: EntryContent,
        onText: (oldRawText: RawText, newRawText: RawText) -> Unit,
    ): Boolean {
        val rawOldText = when (rawOld) {
            is EntryContent.Text -> rawOld.rawText
            EntryContent.Missing -> RawText.EMPTY_TEXT
            else -> null
        }

        val newOldText = when (rawNew) {
            is EntryContent.Text -> rawNew.rawText
            EntryContent.Missing -> RawText.EMPTY_TEXT
            else -> null
        }

        return if (rawOldText != null && newOldText != null) {
            onText(rawOldText, newOldText)
            true
        } else
            false
    }
}