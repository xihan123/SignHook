package website.xihan.signhelper

import androidx.multidex.MultiDexApplication
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.lazyModules
import website.xihan.kv.KVStorage
import website.xihan.kv.KVSyncManager
import website.xihan.signhelper.di.allModules
import website.xihan.signhelper.repository.LocalRepository
import website.xihan.signhelper.util.Const.REDIRECT_PATH
import website.xihan.signhelper.util.Log
import website.xihan.signhelper.util.ModuleConfig.scopes

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 19:33
 * @介绍 :
 */

class MyApp : MultiDexApplication(), KoinComponent {

    val localRepository by inject<LocalRepository>()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MyApp)
            lazyModules(allModules)
        }
//        Log.d("scopes: $scopes")
        KVSyncManager.setTargetPackages(*scopes.toTypedArray())
        KVStorage.getAllKV(REDIRECT_PATH).takeIf { it.isNotEmpty() }?.forEach { (key, value) ->
//            Log.d("key: $key, value: $value")
            if (value != null && value is String) {
                localRepository.updateRedirectPath(key, value)
            }
        }
    }
}