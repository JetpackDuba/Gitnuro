package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.AppEnvInfo
import com.jetpackduba.gitnuro.managers.FlatpakShellManager
import com.jetpackduba.gitnuro.managers.IShellManager
import com.jetpackduba.gitnuro.managers.ShellManager
import com.jetpackduba.gitnuro.system.OS
import com.jetpackduba.gitnuro.system.getCurrentOs
import com.jetpackduba.gitnuro.terminal.ITerminalProvider
import com.jetpackduba.gitnuro.terminal.LinuxTerminalProvider
import com.jetpackduba.gitnuro.terminal.MacTerminalProvider
import com.jetpackduba.gitnuro.terminal.WindowsTerminalProvider
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@Module
class ShellModule {
    @Provides
    fun provideShellManager(
        appEnvInfo: AppEnvInfo,
        shellManager: Provider<ShellManager>,
        flatpakShellManager: Provider<FlatpakShellManager>,
    ): IShellManager {
        return if (appEnvInfo.isFlatpak)
            flatpakShellManager.get()
        else
            shellManager.get()
    }

    @Provides
    fun provideTerminalProvider(
        linuxTerminalProvider: Provider<LinuxTerminalProvider>,
        windowsTerminalProvider: Provider<WindowsTerminalProvider>,
        macTerminalProvider: Provider<MacTerminalProvider>,
    ): ITerminalProvider {
        return when (getCurrentOs()) {
            OS.LINUX -> linuxTerminalProvider.get()
            OS.WINDOWS -> windowsTerminalProvider.get()
            OS.MAC -> macTerminalProvider.get()
            OS.UNKNOWN -> throw NotImplementedError("Unknown operating system")
        }
    }
}