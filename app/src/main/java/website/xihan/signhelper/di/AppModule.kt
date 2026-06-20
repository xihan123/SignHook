package website.xihan.signhelper.di

import android.content.Context
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.lazyModule
import website.xihan.signhelper.data.AppDatabase
import website.xihan.signhelper.data.InstalledAppDao
import website.xihan.signhelper.data.InstalledAppRepository
import website.xihan.signhelper.ui.MainViewModel

val allModules = lazyModule {
    singleOf(::provideRoomDataBase)
    singleOf(::provideApkSignatureDao)
    singleOf(::provideInstalledAppRepository)
    viewModelOf(::MainViewModel)
}

private fun provideRoomDataBase(context: Context): AppDatabase = AppDatabase.getInstance(context)

private fun provideApkSignatureDao(appDatabase: AppDatabase) = appDatabase.installedAppDao()

private fun provideInstalledAppRepository(context: Context, dao: InstalledAppDao) =
    InstalledAppRepository(context, dao)

