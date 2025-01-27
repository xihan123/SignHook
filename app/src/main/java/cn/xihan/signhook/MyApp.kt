package cn.xihan.signhook

import android.app.Application
import androidx.multidex.MultiDexApplication
import cn.xihan.signhook.util.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.lazyModules

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/26 16:12
 * @介绍 :
 */
class MyApp : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApp)
            lazyModules(appModule)
        }
    }

}