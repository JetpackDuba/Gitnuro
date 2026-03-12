package com.jetpackduba.gitnuro.domain.models

import com.jetpackduba.gitnuro.domain.DiffMatchPatch

data class MatchLine(val diffs: List<DiffMatchPatch.Diff>)