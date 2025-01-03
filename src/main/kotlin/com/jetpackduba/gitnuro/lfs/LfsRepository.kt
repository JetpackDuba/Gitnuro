package com.jetpackduba.gitnuro.lfs

import javax.inject.Inject

class LfsRepository @Inject constructor(
    private val lfsNetworkDataSource: LfsNetworkDataSource,
) : ILfsNetworkDataSource by lfsNetworkDataSource