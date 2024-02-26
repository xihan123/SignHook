package cn.xihan.sign.di

import android.content.Context
import cn.xihan.sign.model.ApkSignatureDao
import cn.xihan.sign.repository.LocalRepository
import cn.xihan.sign.ui.MainViewModel
import cn.xihan.sign.utli.AppDatabase
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.includes
import org.koin.dsl.lazyModule


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
    context: Context,
    apkSignatureDao: ApkSignatureDao
): LocalRepository {
    return LocalRepository(
        context = context,
        apkSignatureDao = apkSignatureDao
    )
}


val persistenceModule = lazyModule {
    singleOf(::provideRoomDataBase)
    singleOf(::provideApkSignatureDao)
}

private fun provideRoomDataBase(context: Context): AppDatabase =
    AppDatabase.getInstance(context)

private fun provideApkSignatureDao(appDatabase: AppDatabase) = appDatabase.apkSignatureDao()

val allModules = lazyModule {
    includes(
        appModule,
        persistenceModule
    )
}