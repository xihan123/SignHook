package cn.xihan.sign.ui

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cn.xihan.sign.base.BaseViewModel
import cn.xihan.sign.base.IUiIntent
import cn.xihan.sign.base.IUiState
import cn.xihan.sign.model.ApkSignature
import cn.xihan.sign.repository.LocalRepository
import com.highcapable.yukihookapi.hook.factory.prefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.syntax.simple.repeatOnSubscription

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
                localRepository.queryAllForgedSignature().collect { apkSignatures ->
                    val forgedPackages = apkSignatures
                        .asSequence()
                        .filter { model -> model.isForged && model.forgedSignature.isNotBlank() }
                        .map { model -> model.packageName to model.forgedSignature }
                        .toList()

                    if (forgedPackages.isNotEmpty()) {
                        context.prefs().edit {
                            forgedPackages.forEach { (packageName, forgedSignature) ->
                                putString(packageName, forgedSignature)
                            }
                            putStringSet("packageNames", forgedPackages.map { it.first }.toSet())
                            apply()
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