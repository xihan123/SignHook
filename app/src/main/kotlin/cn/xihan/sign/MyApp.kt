package cn.xihan.sign

import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 19:33
 * @介绍 :
 */
@HiltAndroidApp
class MyApp : MultiDexApplication(),Configuration.Provider {

    @Inject
    lateinit var workerConfiguration: Configuration

    override fun getWorkManagerConfiguration(): Configuration = workerConfiguration
}