package cn.xihan.signhook.util

import android.content.Context
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/26 15:22
 * @介绍 :
 */
class CustomCookiesStorage(
    context: Context
) : CookiesStorage {

    private val mutex = Mutex()

    private val prefs by lazy {
        context.getSharedPreferences("cookie_prefs", Context.MODE_PRIVATE)
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        with(cookie) {
            if (name.isBlank()) return
        }
        mutex.withLock {
            Log.d("requestUrl: $requestUrl \ncookie: $cookie")
            prefs.edit().apply {
                putString("name", cookie.name)
                putString("value", cookie.value)
            }.apply()
        }
    }


    override fun close() {}


    override suspend fun get(requestUrl: Url): List<Cookie> {
        val cookies = mutableListOf<Cookie>()
        mutex.withLock {
            val name = prefs.getString("name", "")
            val value = prefs.getString("value", "")
            if (!name.isNullOrBlank() && !value.isNullOrBlank()) {
                cookies.add(Cookie(name, value))
            }
        }
        return cookies
    }
}