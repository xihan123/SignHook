package website.xihan.signhelper.ui

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import website.xihan.kv.KVStorage
import website.xihan.signhelper.base.BaseViewModel
import website.xihan.signhelper.base.IUiIntent
import website.xihan.signhelper.base.IUiState
import website.xihan.signhelper.model.ApkSignature
import website.xihan.signhelper.repository.LocalRepository
import website.xihan.signhelper.util.Const.FAKE_SIGNATURE
import website.xihan.signhelper.util.Const.REDIRECT_PATH
import website.xihan.signhelper.util.ModuleConfig.scopes

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 20:40
 * @介绍 :
 */
class MainViewModel(
    private val context: Application,
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
        packageName: String, forgedSignature: String
    ) = intent {
        localRepository.updateForgedSignature(packageName, forgedSignature)
    }

    /**
     * 更新本地数据库中的 isForged
     */
    fun updateIsForged(
        packageName: String, isForged: Boolean
    ) = intent {
        localRepository.updateIsForged(packageName, isForged)
    }

    /**
     * 更新本地数据库中的 isRedirect
     */
    fun updateIsRedirect(
        packageName: String, isRedirect: Boolean
    ) = intent {
        localRepository.updateIsRedirect(packageName, isRedirect)
    }

    init {
        querySignature()
        intent {
            repeatOnSubscription {
                localRepository.queryAllForgedSignature().collect { apkSignatures ->
                    val forgedPackages =
                        apkSignatures.filter { it.isForged && it.forgedSignature.isNotBlank() }
                            .associate { it.packageName to it.forgedSignature }

                    forgedPackages.takeIf { it.isNotEmpty() }?.let {
                        scopes = forgedPackages.map { (key, value) -> key }.toSet()
                        KVStorage.apply {
                            clearAll(REDIRECT_PATH)
                            putAll(FAKE_SIGNATURE, it)
                        }
                    }
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