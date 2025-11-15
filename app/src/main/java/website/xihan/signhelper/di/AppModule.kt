package website.xihan.signhelper.di

import android.content.Context
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.includes
import org.koin.dsl.lazyModule
import website.xihan.signhelper.model.ApkSignatureDao
import website.xihan.signhelper.repository.LocalRepository
import website.xihan.signhelper.ui.MainViewModel
import website.xihan.signhelper.util.AppDatabase


/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 19:34
 * @介绍 :
 */
val appModule = lazyModule {
    singleOf(::provideLocalRepository)
    viewModelOf(::MainViewModel)
}

private fun provideLocalRepository(
    context: Context, apkSignatureDao: ApkSignatureDao
): LocalRepository {
    return LocalRepository(
        context = context, apkSignatureDao = apkSignatureDao
    )
}


val persistenceModule = lazyModule {
    singleOf(::provideRoomDataBase)
    singleOf(::provideApkSignatureDao)
}

private fun provideRoomDataBase(context: Context): AppDatabase = AppDatabase.getInstance(context)

private fun provideApkSignatureDao(appDatabase: AppDatabase) = appDatabase.apkSignatureDao()


val allModules = lazyModule {
    includes(
        appModule, persistenceModule
    )
}