package cn.xihan.signhook.util

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/26 18:08
 * @介绍 :
 */
@Keep
sealed interface LoadingIntent : IUiIntent {
    data object IDLE : LoadingIntent
    data object LOADING : LoadingIntent
    data class FAILURE(val error: AgeException) : LoadingIntent
}

@Keep
interface IUiState {
    var loading: Boolean
    var refreshing: Boolean
    var error: AgeException?
}


@Keep
interface IUiIntent

abstract class BaseViewModel<S : IUiState, I : IUiIntent> : ContainerHost<S, I>, ViewModel() {

    val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.i("${this::class.java.simpleName} exception: $exception")
        val errorResponse = if (exception is AgeException) {
            exception
        } else {
            AgeException.SnackBarException(message = exception.message ?: "未知错误")
        }
        showError(errorResponse)
    }

    override val container: Container<S, I> by lazy {
        container(
            initialState = initViewState(),
            buildSettings = {
                exceptionHandler = coroutineExceptionHandler
            }
        )
    }


    abstract fun initViewState(): S

    open fun hideError() {}

    open fun showError(error: AgeException) {}

}