package cn.xihan.sign.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cn.xihan.sign.repository.LocalRepository
import cn.xihan.sign.utli.loge
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.catch

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 20:13
 * @介绍 :
 */
@HiltWorker
class ApkInfoWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val localRepository: LocalRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        localRepository.getAllApkSignature()
            .catch {
                "ApkInfoWorker error: ${it.message}".loge()
            }
            .collect {
                localRepository.updateApkSignature(it)
            }
        return Result.success()
    }

}