package website.xihan.signhelper.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.annotation.WorkerThread
import androidx.paging.Pager
import androidx.paging.PagingConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import website.xihan.signhelper.model.ApkSignature
import website.xihan.signhelper.model.ApkSignatureDao
import website.xihan.signhelper.util.Log
import website.xihan.signhelper.util.getSignature

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 19:52
 * @介绍 :
 */
@WorkerThread
class LocalRepository(
    private val context: Context, private val apkSignatureDao: ApkSignatureDao
) {

    init {
        Log.d("Injection LocalRepository")
    }

    /**
     * 查询本地所有的 Apk 信息 并更新数据库
     */
    fun getAllApkSignature() = flow {
        val appSignatureList = mutableListOf<ApkSignature>()
        // 从本机获取所有的 Apk 信息 排除系统应用
        val list = context.packageManager.getInstalledPackages(0)
            .filter { (it.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM == 0) }
            .filterNotNull()
        if (list.isNotEmpty()) {
            list.forEach {
                val apkName = it.applicationInfo!!.loadLabel(context.packageManager).toString()
                val packageName = it.packageName
                val signature = context.getSignature(packageName)
                appSignatureList += ApkSignature(apkName, packageName, signature)
            }
        }

        emit(appSignatureList.ifEmpty { emptyList() })
    }.flowOn(Dispatchers.IO)

    /**
     * 更新数据库
     */
    suspend fun updateApkSignature(apkSignature: List<ApkSignature>) {
        apkSignature.forEach { it ->
            apkSignatureDao.queryApkSignature(it.packageName)?.let {
                apkSignatureDao.updateApkSignature(
                    apkName = it.apkName,
                    packageName = it.packageName,
                    originalSignature = it.originalSignature,
                )
            } ?: apkSignatureDao.upsert(it)
        }

    }

    /**
     * 分页查询本地数据
     */
    fun queryAppInfoModel(query: String = "") = Pager(PagingConfig(pageSize = 24)) {
        if (query.isBlank()) {
            apkSignatureDao.queryAllAppInfoModelOrderByForgedSignature()
        } else {
            apkSignatureDao.queryAppInfoModel(query)
        }
    }

    /**
     * 查询所有 forgedSignature 不为空的数据
     */
    fun queryAllForgedSignature() = apkSignatureDao.queryAllAppInfoModelForged()

    /**
     * 更新 forgedSignature
     */
    suspend fun updateForgedSignature(packageName: String, forgedSignature: String) =
        apkSignatureDao.updateForgedSignature(packageName, forgedSignature)

    /**
     * 更新 isForged
     */
    suspend fun updateIsForged(packageName: String, isForged: Boolean) =
        apkSignatureDao.updateIsForged(packageName, isForged)

    /**
     * 更新指定包名的重定向状态
     *
     * @param packageName 应用包名
     * @param isRedirect 重定向状态标识
     */
    suspend fun updateIsRedirect(packageName: String, isRedirect: Boolean) =
        apkSignatureDao.updateIsRedirect(packageName, isRedirect)

    /**
     * 获取指定包名的重定向路径
     *
     * @param packageName 应用包名
     * @return 重定向路径
     */
    suspend fun getRedirectPath(packageName: String): String? =
        apkSignatureDao.queryApkSignature(packageName)?.redirectPath

    /**
     * 更新指定包名的重定向路径
     *
     * @param packageName 应用包名
     * @param redirectPath 重定向路径
     */
    fun updateRedirectPath(packageName: String, redirectPath: String) =
        CoroutineScope(Dispatchers.IO).launch {
            apkSignatureDao.updateRedirectPath(packageName, redirectPath)
        }


}