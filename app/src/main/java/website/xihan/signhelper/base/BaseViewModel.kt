package website.xihan.signhelper.base

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import website.xihan.signhelper.util.Const.TAG


/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 19:41
 * @介绍 :
 */
@Keep
interface IUiState {
    var loading: Boolean
    var refreshing: Boolean
    var error: Throwable?
}

@Keep
interface IUiIntent

@Keep
interface IViewModel<S : IUiState, I : IUiIntent> : ContainerHost<S, I> {
    fun initViewState(): S
}

abstract class BaseViewModel<S : IUiState, I : IUiIntent> : IViewModel<S, I>, ViewModel() {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "${exception.message}")

        showError(exception)
    }

    override val container: Container<S, I> = container(
        initialState = initViewState(), buildSettings = {
            exceptionHandler = coroutineExceptionHandler
        })

    open fun hideError() {}

    open fun showError(error: Throwable) {}

    /**
     * 主线程执行
     */
    fun mainLaunch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(Dispatchers.Main + coroutineExceptionHandler) {
            block.invoke(this)
        }
    }

    /**
     * IO线程执行
     */
    fun ioLaunch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            block.invoke(this)
        }
    }

}


