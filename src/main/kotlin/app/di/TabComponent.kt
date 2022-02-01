package app.di

import app.ui.components.TabInformation
import dagger.Component

@TabScope
@Component(dependencies = [AppComponent::class])
interface TabComponent {
    fun inject(tabInformation: TabInformation)
}