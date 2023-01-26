package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.git.TabState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class BranchFilterViewModel @Inject constructor(
    private val tabState: TabState
) {
    val keyword: StateFlow<String> = tabState.branchFilterKeyword

    fun newBranchFilter(keyword: String) {
        tabState.newBranchFilter(keyword)
    }
}