package cn.xihan.sign.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import cn.xihan.sign.repository.LocalRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 20:12
 * @介绍 :
 */
@Singleton
class MyWorkerFactory @Inject constructor(
    private val localRepository: LocalRepository,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            ApkInfoWorker::class.java.name -> ApkInfoWorker(
                appContext,
                workerParameters,
                localRepository
            )
            else -> null
        }
    }


}