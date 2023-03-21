package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.AppEnvInfo
import com.jetpackduba.gitnuro.extensions.OS
import com.jetpackduba.gitnuro.extensions.getCurrentOs
import com.jetpackduba.gitnuro.terminal.*
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@Module
class TerminalModule {
    @Provides
    fun provideTerminalProvider(
        linuxTerminalProvider: Provider<LinuxTerminalProvider>,
        windowsTerminalProvider: Provider<WindowsTerminalProvider>,
        macTerminalProvider: Provider<MacTerminalProvider>,
        flatpakTerminalProvider: Provider<FlatpakTerminalProvider>,
        appEnvInfo: AppEnvInfo,
    ): ITerminalProvider {

        if (appEnvInfo.isFlatpak)
            return flatpakTerminalProvider.get()

        return when (getCurrentOs()) {
            OS.LINUX -> linuxTerminalProvider.get()
            OS.WINDOWS -> windowsTerminalProvider.get()
            OS.MAC -> macTerminalProvider.get()
            OS.UNKNOWN -> throw NotImplementedError("Unknown operating system")
        }
    }
}