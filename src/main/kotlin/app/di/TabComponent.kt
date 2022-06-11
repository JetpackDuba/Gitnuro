package app.di

import app.AppPreferences
import app.di.modules.NetworkModule
import app.ui.components.TabInformation
import dagger.Component

@TabScope
@Component(
    modules = [
        NetworkModule::class,
    ],
    dependencies = [
        AppComponent::class
    ],
)
interface TabComponent {
    fun inject(tabInformation: TabInformation)
}