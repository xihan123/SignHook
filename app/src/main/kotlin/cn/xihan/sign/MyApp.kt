package cn.xihan.sign

import androidx.multidex.MultiDexApplication
import cn.xihan.sign.di.allModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.lazyModules
import org.koin.core.logger.Level

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 19:33
 * @介绍 :
 */

class MyApp : MultiDexApplication(), KoinComponent {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MyApp)
            lazyModules(allModules)
        }
    }
}