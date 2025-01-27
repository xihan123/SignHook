package cn.xihan.signhook.util

import android.content.Context
import cn.xihan.signhook.ui.MainViewModel
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.lazyModule

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/26 15:07
 * @介绍 :
 */
const val API_PATH = "https://api.signhelper.sbs/"

val appModule = lazyModule {
    singleOf(::provideHttpClient)
    singleOf(::provideKtorfit)
    singleOf(::provideHttpCookies)
    singleOf(::provideApiService)
    singleOf(::provideSharedPreference)
    singleOf(::RemoteRepository)
    viewModelOf(::MainViewModel)
}

private fun provideHttpCookies(context: Context) = CustomCookiesStorage(context)

private fun provideHttpClient(cookiesStorage: CustomCookiesStorage): HttpClient =
    HttpClient(OkHttp) {
//        expectSuccess = true
        install(HttpCookies) {
            storage = cookiesStorage
        }
        install(ContentNegotiation) {
            json(kJson)
        }
//        HttpResponseValidator {
//            handleResponseExceptionWithRequest { exception, request ->
////                Log.d("Error Url: ${request.url}, ${exception.message}")
////                throw Throwable(exception.message ?: "Unknown error")
//            }
//        }
        install(ContentEncoding) {
            deflate(1.0F)
            gzip(0.9F)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 5000
            connectTimeoutMillis = 5000
            socketTimeoutMillis = 5000
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d(message)
                }
            }
            level = LogLevel.ALL
        }
    }

private fun provideKtorfit(httpClient: HttpClient) =
    Ktorfit.Builder().httpClient(httpClient).baseUrl(API_PATH).build()

private fun provideApiService(ktorfit: Ktorfit) = ktorfit.createApiService()

private fun provideSharedPreference(context: Context) =
    context.getSharedPreferences("module_config", Context.MODE_PRIVATE)

