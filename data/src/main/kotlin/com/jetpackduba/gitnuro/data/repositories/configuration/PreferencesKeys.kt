package com.jetpackduba.gitnuro.data.repositories.configuration

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKeys {
    val scaleUiPreference get() = floatPreferencesKey("scale_ui")
    val themePreference get() = stringPreferencesKey("theme")
    val customThemePreference get() = stringPreferencesKey("custom_theme")
    val linesHeightPreference get() = stringPreferencesKey("lines_height")
    val swapStatusPanesPreference get() = booleanPreferencesKey("swap_status_panes")
    val diffDisplayFullFilePreference get() = booleanPreferencesKey("diff_display_full_file")
    val groupBranchesWithRemotesPreference get() = booleanPreferencesKey("group_branches_with_remotes")
    val diffTextViewTypePreference get() = stringPreferencesKey("diff_text_view")
    val showChangesAsTreePreference get() = booleanPreferencesKey("show_changes_as_tree")

    val dateFormatUseDefaultPreference get() = booleanPreferencesKey("date_format_use_default")
    val dateFormatCustomFormatPreference get() = stringPreferencesKey("date_format_custom_format")
    val dateFormatIs24hPreference get() = booleanPreferencesKey("date_format_is_24h")
    val dateFormatUseRelativePreference get() = booleanPreferencesKey("date_format_use_relative")

    val avatarProviderPreference get() = stringPreferencesKey("avatar_provider")

    val fastForwardMergePreference get() = booleanPreferencesKey("fast_forward_merge")
    val autoStashOnMergePreference get() = booleanPreferencesKey("auto_stash_on_merge")
    val pullWithRebasePreference get() = booleanPreferencesKey("pull_with_rebase")
    val pushWithLeasePreference get() = booleanPreferencesKey("push_with_lease")
    val cloneDefaultDirectoryPreference get() = stringPreferencesKey("clone_default_directory")

    val useProxyPreference get() = booleanPreferencesKey("use_proxy")
    val proxyUseAuthPreference get() = booleanPreferencesKey("proxy_use_auth")
    val proxyProxyTypePreference get() = intPreferencesKey("proxy_type")
    val proxyHostNamePreference get() = stringPreferencesKey("proxy_host_name")
    val proxyPortNumberPreference get() = intPreferencesKey("proxy_port_number")
    val proxyHostUserPreference get() = stringPreferencesKey("proxy_host_user")
    val proxyHostPasswordPreference get() = stringPreferencesKey("proxy_host_password")
    val cacheCredentialsPreference get() = booleanPreferencesKey("cache_credentials_in_memory")

    val verifySslPreference get() = booleanPreferencesKey("verify_ssl")

    val terminalPathPreference get() = stringPreferencesKey("terminal_path")

    val windowPlacementPreference get() = intPreferencesKey("window_placement")
    val openTabsPreference get() = stringPreferencesKey("open_tabs")
    val currentTabIndexPreference get() = intPreferencesKey("current_tab_index")
    val firstPaneWidthPreference get() = floatPreferencesKey("first_pane_width")
    val thirdPaneWidthPreference get() = floatPreferencesKey("third_pane_width")
}