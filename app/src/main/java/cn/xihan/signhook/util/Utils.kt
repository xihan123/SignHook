package cn.xihan.signhook.util

import android.app.Activity
import android.app.AndroidAppHelper
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.view.ViewGroup
import android.widget.Toast
import cn.xihan.signhook.model.isSuccess
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import android.util.Log as ALog

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/21 16:30
 * @介绍 :
 */
val kJson = Json {
    isLenient = true
//    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
    coerceInputValues = true
}

val currentContext by lazy { AndroidAppHelper.currentApplication() as Context }

val packageName: String by lazy { currentContext.packageName }

@Suppress("DEPRECATION")
val sPrefs: SharedPreferences
    get() = currentContext.getSharedPreferences("packages", Context.MODE_MULTI_PROCESS)

inline fun <reified T> Any?.safeCast(): T? = this as? T

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

infix fun Int.x(other: Int): ViewGroup.LayoutParams = ViewGroup.LayoutParams(this, other)

fun Context.copyToClipboard(text: String) {
    // 获取系统服务中的剪贴板管理器
    val clipboardManager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    // 将文本创建为新的剪贴板数据，并设置到剪贴板中
    clipboardManager.setPrimaryClip(ClipData.newPlainText(null, text))
}

object Log {

    private const val TAG = "LSPosed-Bridge"

    @JvmStatic
    private fun doLog(f: (String, String) -> Int, obj: Any?, toXposed: Boolean = false) {
        val str = if (obj is Throwable) ALog.getStackTraceString(obj) else obj.toString()

        if (str.length > maxLength) {
            val chunkCount: Int = str.length / maxLength
            for (i in 0..chunkCount) {
                val max: Int = maxLength * (i + 1)
                if (max >= str.length) {
                    doLog(f, str.substring(maxLength * i))
                } else {
                    doLog(f, str.substring(maxLength * i, max))
                }
            }
        } else {
            f(TAG, str)
            if (toXposed) XposedBridge.log("$TAG : $str")
        }
    }

    @JvmStatic
    fun d(obj: Any?) {
        doLog(ALog::d, obj)
    }

    @JvmStatic
    fun i(obj: Any?) {
        doLog(ALog::i, obj)
    }

    @JvmStatic
    fun e(obj: Any?) {
        doLog(ALog::e, obj, true)
    }

    @JvmStatic
    fun v(obj: Any?) {
        doLog(ALog::v, obj)
    }

    @JvmStatic
    fun w(obj: Any?) {
        doLog(ALog::w, obj)
    }

    private const val maxLength = 3000

}

object Utils : KoinComponent {

    // 匹配的类名集合
    val classNameList = setOf(
        "AgentActivity",
        "SplashActivity",
        "PublicFragmentActivityForOpenSDK",
        "WXEntryActivity",
        "LauncherUI"
    )


    val ctx by inject<Context>()
    val packageSharedPreferences by lazy {
        ctx.getSharedPreferences("packages", Context.MODE_PRIVATE)
    }
    private val remoteRepository by inject<RemoteRepository>()
    private var scopeRef: AtomicReference<Any> = AtomicReference()
    val appGlobalScope: CoroutineScope
        get() {
            while (true) {
                val existing = scopeRef.get() as CoroutineScope?
                if (existing != null) {
                    return existing
                }
                val newScope = SafeCoroutineScope(Dispatchers.Main.immediate)
                if (scopeRef.compareAndSet(null, newScope)) {
                    return newScope
                }
            }
        }

    private class SafeCoroutineScope(context: CoroutineContext) : CoroutineScope, Closeable {
        override val coroutineContext: CoroutineContext =
            SupervisorJob() + context + UncaughtCoroutineExceptionHandler()

        override fun close() {
            coroutineContext.cancelChildren()
        }
    }

    // 自定义 CoroutineExceptionHandler
    private class UncaughtCoroutineExceptionHandler : CoroutineExceptionHandler,
        AbstractCoroutineContextElement(CoroutineExceptionHandler) {
        override fun handleException(context: CoroutineContext, exception: Throwable) {
            // 吐司错误
//            ctx.toast("发生未捕获的异常，请联系开发者")
            // 打印错误日志
            Log.e("UncaughtCoroutineExceptionHandler: $exception")
        }
    }


    fun checkDialogStatus(activity: Activity) = appGlobalScope.launch(Dispatchers.IO) {
        val result = remoteRepository.checkDialogStatus()
        if (result.isSuccess() && result.data != null) {
            if (result.data == 1) {
                getUserPackages(activity)
                withContext(Dispatchers.Main.immediate) {
                    activity.toast("弹框状态: ${result.data}")
                }
            }
        } else {
            if (result.message.contains("token 无效")) {
                showLoginDialog(activity)
            }
        }
    }

    fun showLoginDialog(activity: Activity) = appGlobalScope.launch(Dispatchers.Main.immediate) {
        val email = CustomEditText(
            context = activity,
            value = HostSettings.email,
            hint = "请输入邮箱",
            title = "邮箱",
        )

        val password = CustomEditText(
            context = activity,
            value = HostSettings.password,
            hint = "请输入密码",
            title = "密码",
            isPassword = true
        )

        val linearLayout = CustomLinearLayout(
            context = activity,
            isAutoWidth = true,
            isAutoHeight = true,
        ).also {
            it.addView(email)
            it.addView(password)
        }

        fun login() = appGlobalScope.launch(Dispatchers.IO) {
            val result = remoteRepository.login(
                email.editText.text.toString(),
                password.editText.text.toString()
            )
            if (result.isSuccess()) {
                HostSettings.apply {
                    isLogin = true
                    this.email = email.editText.text.toString()
                    this.password = password.editText.text.toString()
                }
                getUserPackages(activity)
            }
            withContext(Dispatchers.Main.immediate) {
                activity.toast(result.message)
            }
        }

        activity.alertDialog {
            title = "登录"
            message = "请输入邮箱和密码"
            customView = linearLayout
            positiveButton("登录") {
                login()
            }
            build()
            show()
        }

    }

    fun getUserPackages(activity: Activity) = appGlobalScope.launch(Dispatchers.IO) {
        val result = remoteRepository.getUserPackages()
        if (result.isSuccess() && !result.data.isNullOrEmpty()) {
            val edit = packageSharedPreferences.edit()
            edit.clear()
            result.data.forEach {
                edit.putString(it.packageName, it.signatureValue)
            }
            edit.apply()
            withContext(Dispatchers.Main.immediate) {
                activity.toast("获取到 ${result.data.size} 个应用包名")
            }
        } else {
            Log.e("getUserPackages: ${result.message}")
            withContext(Dispatchers.Main.immediate) {
                ctx.toast(result.message)
            }
        }
    }
}