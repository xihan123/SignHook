package cn.xihan.signhook.ui

import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cn.xihan.signhook.model.TitleAndPackageNameModel
import cn.xihan.signhook.model.isSuccess
import cn.xihan.signhook.util.AgeException
import cn.xihan.signhook.util.BaseViewModel
import cn.xihan.signhook.util.IUiIntent
import cn.xihan.signhook.util.IUiState
import cn.xihan.signhook.util.ModuleSettings
import cn.xihan.signhook.util.ModuleSettings.isLogin
import cn.xihan.signhook.util.RemoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/26 16:17
 * @介绍 :
 */
class MainViewModel(
    private val sharedPreferences: SharedPreferences,
    private val remoteRepository: RemoteRepository
) : BaseViewModel<MainState, MainIntent>() {


    override fun initViewState(): MainState = MainState()


    fun register(email: String) = intent {
        val result = remoteRepository.register(email)
        if (result.isSuccess() && result.message == "注册成功") {
            ModuleSettings.email = email
        }
        loginStatus(result.message)
        showError(AgeException.SnackBarException(message = result.message))
    }

    fun login(email: String, password: String) = intent {
        val result = remoteRepository.login(email, password)
        if (result.isSuccess() && result.message == "登录成功") {
            ModuleSettings.email = email
            ModuleSettings.password = password
        }
        loginStatus(result.message)
        showError(AgeException.SnackBarException(message = result.message, code = result.code))
    }

    fun logout() = intent {
        remoteRepository.logout()
        isLogin = false
        sharedPreferences.edit().clear().apply()
        reduce {
            state.copy(isLogin = false)
        }
    }

    fun checkDialogStatus() = intent {
        val result = remoteRepository.checkDialogStatus()
        if (result.isSuccess() && result.data != null) {
            reduce {
                state.copy(showDialog = result.data == 1)
            }
        } else {
            if (result.message.contains("token 无效")) {
                logout()
            }
            showError(AgeException.SnackBarException(message = result.message, code = result.code))
        }
    }

    fun updateDialogStatus(status: Int) = intent {
        val result = remoteRepository.updateDialogStatus(status)
        if (result.isSuccess() && result.message == "更新成功") {
            reduce {
                state.copy(showDialog = status == 1)
            }

        }
        showError(AgeException.SnackBarException(message = result.message, code = result.code))
    }

    fun submitPackages(packageName: String) = intent {
        val result = remoteRepository.submitPackages(packageName)
        if (result.isSuccess() && result.message == "提交成功") {
            val packages = (state.selectedPackages + packageName).toMutableSet()
            sharedPreferences.edit().putStringSet("packages", packages).apply()
            reduce {
                state.copy(selectedPackages = packages)
            }
        }
        showError(AgeException.SnackBarException(message = result.message, code = result.code))
    }

    fun deletePackage(packageName: String) = intent {
        val result = remoteRepository.deletePackage(packageName)
        if (result.isSuccess() && result.message == "删除成功") {
            val packages = (state.selectedPackages - packageName).toMutableSet()
            sharedPreferences.edit().putStringSet("packages", packages).apply()
            reduce {
                state.copy(selectedPackages = packages)
            }
        }
        showError(AgeException.SnackBarException(message = result.message, code = result.code))
    }

    fun getUserPackages() = intent {
        val result = remoteRepository.getUserPackages()
        if (result.isSuccess() && result.data != null) {
            val packages = result.data.map { it.packageName }.toSet()
            reduce {
                state.copy(selectedPackages = packages)
            }
            sharedPreferences.edit().putStringSet("packages", packages).apply()
        } else {
            reduce {
                state.copy(
                    selectedPackages = sharedPreferences.getStringSet("packages", setOf())
                        ?: setOf()
                )
            }
        }
        showError(AgeException.SnackBarException(message = result.message, code = result.code))
    }

    fun search(query: String = "") = intent {
        reduce {
            state.copy(
                currentQuery = query,
                signatures = remoteRepository.searchPackages(query).cachedIn(viewModelScope),
            )
        }
    }

    override fun showError(error: AgeException) {
        intent {
            reduce {
                state.copy(error = error)
            }
        }
    }

    override fun hideError() {
        intent {
            reduce {
                state.copy(error = null)
            }
        }
    }

    fun showLoading() = intent {
        reduce {
            state.copy(loading = true)
        }
    }

    fun onRefresh() = intent {
        search(state.currentQuery)
        getUserPackages()
        checkDialogStatus()
    }

    private fun loginStatus(message: String) = intent {
        val isSuccess = message == "登录成功" || message == "注册成功"
        if (isSuccess) {
            onRefresh()
        }
        isLogin = isSuccess
        reduce {
            state.copy(isLogin = isSuccess)
        }
    }

    init {
        if (isLogin) {
            onRefresh()
        }
    }

}

data class MainState(
    override var loading: Boolean = false,
    override var refreshing: Boolean = false,
    override var error: AgeException? = null,
    val currentQuery: String = "",
    val signatures: Flow<PagingData<TitleAndPackageNameModel>> = flowOf(PagingData.empty()),
    val selectedPackages: Set<String> = setOf(),
    val showDialog: Boolean = false,
    val isLogin: Boolean = ModuleSettings.isLogin
) : IUiState

sealed class MainIntent : IUiIntent {

}