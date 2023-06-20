package cn.xihan.sign.ui

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cn.xihan.sign.base.BaseViewModel
import cn.xihan.sign.base.IUiIntent
import cn.xihan.sign.base.IUiState
import cn.xihan.sign.hook.HookEntry.Companion.optionModel
import cn.xihan.sign.model.ApkSignature
import cn.xihan.sign.repository.LocalRepository
import cn.xihan.sign.utli.writeConfigFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.syntax.simple.repeatOnSubscription
import javax.inject.Inject

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 20:40
 * @介绍 :
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val localRepository: LocalRepository,
) : BaseViewModel<MainState, IUiIntent>() {

    override fun initViewState(): MainState = MainState()

    /**
     * 更新本地数据库中的Signature记录列表
     */
    fun updateSignatureList() = intent {
        localRepository.getAllApkSignature().collect {
            localRepository.updateApkSignature(it)
        }
    }

    /**
     * 查询本地数据库中的Signature记录列表
     */
    fun querySignature(query: String = "") = intent {
        reduce {
            state.copy(
                apkSignatureFlow = localRepository.queryAppInfoModel(query).flow.cachedIn(
                    viewModelScope
                )
            )
        }
    }

    /**
     * 更新本地数据库中的 fakeSignature
     */
    fun updateForgedSignature(
        packageName: String,
        forgedSignature: String
    ) = intent {
        localRepository.updateForgedSignature(packageName, forgedSignature)
    }

    /**
     * 更新本地数据库中的 isForged
     */
    fun updateIsForged(
        packageName: String,
        isForged: Boolean
    ) = intent {
        localRepository.updateIsForged(packageName, isForged)
    }

    init {
        querySignature()
        intent {
            repeatOnSubscription {
                localRepository.queryAllForgedSignature().collect {
                    optionModel.apkSignatureList = it
                    writeConfigFile()
                }
            }
        }
    }

}


data class MainState(
    override var loading: Boolean = false,
    override var refreshing: Boolean = false,
    override var error: Throwable? = null,
    val apkSignatureFlow: Flow<PagingData<ApkSignature>> = flowOf(PagingData.empty()),
) : IUiState