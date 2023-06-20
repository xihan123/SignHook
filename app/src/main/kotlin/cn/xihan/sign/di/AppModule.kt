package cn.xihan.sign.di

import android.content.Context
import android.content.res.Resources
import androidx.work.Configuration
import cn.xihan.sign.model.ApkSignatureDao
import cn.xihan.sign.repository.LocalRepository
import cn.xihan.sign.work.MyWorkerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 19:34
 * @介绍 :
 */
@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Provides
    @Singleton
    fun resources(@ApplicationContext context: Context): Resources = context.resources

    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(
        workerFactory: MyWorkerFactory,
    ): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    @Provides
    fun provideLocalRepository(
        @ApplicationContext context: Context,
        apkSignatureDao: ApkSignatureDao
    ): LocalRepository {
        return LocalRepository(
            context = context,
            apkSignatureDao = apkSignatureDao
        )
    }
}


