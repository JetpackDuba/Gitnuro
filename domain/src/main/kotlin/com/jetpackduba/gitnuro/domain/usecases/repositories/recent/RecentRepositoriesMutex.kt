package com.jetpackduba.gitnuro.domain.usecases.repositories.recent

import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

class RecentRepositoriesMutex @Inject constructor() : Mutex by Mutex()