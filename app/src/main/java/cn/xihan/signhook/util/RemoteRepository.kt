package cn.xihan.signhook.util

import androidx.paging.Pager
import androidx.paging.PagingConfig
import cn.xihan.signhook.model.LoginRequest
import cn.xihan.signhook.model.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/26 16:41
 * @介绍 :
 */
class RemoteRepository(
    private val apiService: ApiService
) {

    private suspend fun <T> apiService(block: suspend ApiService.() -> T): T = apiService.block()

    suspend fun register(email: String) = apiService {
        register(RegisterRequest(email))
    }

    suspend fun login(email: String, password: String) = apiService {
        login(LoginRequest(email, password))
    }

    suspend fun logout() = apiService {
        logout()
    }

    suspend fun checkDialogStatus() = apiService {
        checkDialogStatus()
    }

    suspend fun updateDialogStatus(status: Int) = apiService {
        updateDialogStatus(status)
    }

    suspend fun getUserPackages() = apiService {
        getUserPackages()
    }

    suspend fun submitPackages(packageName: String) = apiService {
        submitPackages(packageName)
    }

    suspend fun deletePackage(packageName: String) = apiService {
        deletePackage(packageName)
    }

    fun searchPackages(keyword: String?) = Pager(
        config = PagingConfig(
            pageSize = 15,
            prefetchDistance = 5,
            initialLoadSize = 15
        ),
        pagingSourceFactory = {
            SignaturePagingSource(
                apiService = apiService,
                keyword = keyword
            )
        }
    ).flow.flowOn(Dispatchers.IO)

}