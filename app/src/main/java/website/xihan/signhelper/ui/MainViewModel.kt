package website.xihan.signhelper.ui

import android.content.Context
import io.github.libxposed.service.XposedService
import website.xihan.signhelper.MyApp
import website.xihan.signhelper.R
import website.xihan.signhelper.base.BaseViewModel
import website.xihan.signhelper.base.IUiIntent
import website.xihan.signhelper.base.IUiState
import website.xihan.signhelper.data.InstalledAppEntity
import website.xihan.signhelper.data.InstalledAppRepository
import website.xihan.signhelper.util.Settings
import java.util.Locale

sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    data class Resource(val resId: Int) : UiText
}

fun UiText.asString(context: Context): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> context.getString(resId)
}

data class MainUiState(
    val apps: List<InstalledAppEntity> = emptyList(),
    val sourceApps: List<InstalledAppEntity> = emptyList(),
    val totalAppCount: Int = 0,
    val enabledFakeCount: Int = 0,
    val query: String = "",
    val hideSystemApps: Boolean = readHideSystemAppsSetting(),
    val hideAppIcons: Boolean = readHideAppIconsSetting(),
    override var loading: Boolean = false,
    override var refreshing: Boolean = false,
    override var error: Throwable? = null
) : IUiState

sealed interface MainUiIntent : IUiIntent {
    data class ShowError(val message: UiText) : MainUiIntent
}

class MainViewModel(
    private val repository: InstalledAppRepository
) : BaseViewModel<MainUiState, MainUiIntent>(), MyApp.ServiceStateListener {

    init {
        observeApps()
        MyApp.addServiceStateListener(this, notifyImmediately = true)
        refresh()
    }

    override fun initViewState(): MainUiState = MainUiState()

    override fun showError(error: Throwable) {
        intent {
            reduce { state.copy(error = error) }
            postSideEffect(MainUiIntent.ShowError(error.toUiText(R.string.unknown_error)))
        }
    }

    override fun hideError() {
        intent {
            reduce { state.copy(error = null) }
        }
    }

    override fun onServiceStateChanged(service: XposedService?) {
        if (service == null) return
        intent {
            reduce {
                state.copy(
                    hideSystemApps = readHideSystemAppsSetting(),
                    hideAppIcons = readHideAppIconsSetting()
                ).filtered()
            }
        }
    }

    override fun onCleared() {
        MyApp.removeServiceStateListener(this)
    }

    fun onQueryChange(value: String) {
        intent {
            reduce {
                state.copy(query = value).filtered()
            }
        }
    }

    fun clearQuery() {
        onQueryChange("")
    }

    fun onHideSystemAppsChange(value: Boolean) {
        writeSetting { hideSystemApps = value }
        intent {
            reduce {
                state.copy(hideSystemApps = value).filtered()
            }
        }
    }

    fun onHideAppIconsChange(value: Boolean) {
        writeSetting { hideAppIcons = value }
        intent {
            reduce {
                state.copy(hideAppIcons = value)
            }
        }
    }

    fun refresh() {
        intent {
            reduce {
                state.copy(refreshing = true, error = null)
            }
        }

        ioLaunch {
            runCatching {
                repository.refreshInstalledApps()
            }.onFailure { error ->
                intent {
                    reduce {
                        state.copy(error = error)
                    }
                    postSideEffect(
                        MainUiIntent.ShowError(error.toUiText(R.string.error_refresh_apps_failed))
                    )
                }
            }

            intent {
                reduce {
                    state.copy(refreshing = false)
                }
            }
        }
    }

    fun updateFakeSignature(packageName: String, fakeSignature: String) {
        ioLaunch {
            runCatching {
                repository.updateFakeSignature(packageName, fakeSignature.trim())
            }.onFailure { error ->
                intent {
                    reduce {
                        state.copy(error = error)
                    }
                    postSideEffect(
                        MainUiIntent.ShowError(error.toUiText(R.string.error_save_fake_signature_failed))
                    )
                }
            }
        }
    }

    fun updateFakeSignatureEnabled(packageName: String, enabled: Boolean) {
        ioLaunch {
            runCatching {
                repository.updateFakeSignatureEnabled(packageName, enabled)
            }.onFailure { error ->
                intent {
                    reduce {
                        state.copy(error = error)
                    }
                    postSideEffect(
                        MainUiIntent.ShowError(error.toUiText(R.string.error_save_enabled_state_failed))
                    )
                }
            }
        }
    }

    private fun observeApps() {
        ioLaunch {
            repository.observeApps().collect { apps ->
                intent {
                    reduce {
                        state.copy(
                            sourceApps = apps,
                            totalAppCount = apps.size,
                            enabledFakeCount = apps.count {
                                it.fakeSignatureEnabled && it.fakeSignature.isNotBlank()
                            }).filtered()
                    }
                }
            }
        }
    }

    private fun MainUiState.filtered(): MainUiState {
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        val filteredApps =
            sourceApps.asSequence().filter { app -> !hideSystemApps || !app.isSystemApp }
                .filter { app ->
                    normalizedQuery.isBlank() || app.appName.lowercase(Locale.getDefault())
                        .contains(normalizedQuery) || app.packageName.lowercase(Locale.US)
                        .contains(normalizedQuery)
                }.sortedWith(appSorter()).toList()

        return copy(apps = filteredApps)
    }

    private fun appSorter(): Comparator<InstalledAppEntity> =
        compareByDescending<InstalledAppEntity> {
            it.fakeSignatureEnabled && it.fakeSignature.isNotBlank()
        }.thenBy {
            it.appName.lowercase(Locale.getDefault())
        }.thenBy {
            it.packageName.lowercase(Locale.US)
        }

    private fun Throwable.toUiText(fallbackResId: Int): UiText =
        message?.let(UiText::Dynamic) ?: UiText.Resource(fallbackResId)
}

private fun readHideSystemAppsSetting(): Boolean =
    runCatching { Settings.hideSystemApps }.getOrDefault(true)

private fun readHideAppIconsSetting(): Boolean =
    runCatching { Settings.hideAppIcons }.getOrDefault(false)

private inline fun writeSetting(block: Settings.() -> Unit) {
    runCatching { Settings.block() }
}
