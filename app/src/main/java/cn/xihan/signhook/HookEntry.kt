package cn.xihan.signhook

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.os.Bundle
import androidx.core.content.edit
import cn.xihan.signhook.util.HostSettings
import cn.xihan.signhook.util.Log
import cn.xihan.signhook.util.Utils
import cn.xihan.signhook.util.Utils.checkDialogStatus
import cn.xihan.signhook.util.Utils.showLoginDialog
import cn.xihan.signhook.util.appModule
import cn.xihan.signhook.util.getSharedPreferences
import cn.xihan.signhook.util.hookAfterMethod
import cn.xihan.signhook.util.hookBeforeMethod
import cn.xihan.signhook.util.sPrefs
import cn.xihan.signhook.util.safeCast
import cn.xihan.signhook.util.setObjectField
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.json.JSONObject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.lazyModules
import java.io.File
import java.util.zip.ZipFile

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/21 16:21
 * @介绍 :
 */
class HookEntry : IXposedHookLoadPackage {

    companion object {
        // 缓存已读取的签名，避免重复读取APK文件
        // 使用特定前缀区分签名缓存和其他用途的包名key
        private val signatureCache = hashMapOf<String, String>()
        private const val SIGNATURE_KEY_PREFIX = "signature_"

        /**
         * 启动时从sPrefs填充缓存
         */
        fun initSignatureCache(application: Application) {
            try {
                val allPrefs = application.getSharedPreferences().all
                Log.d("allPrefs: $allPrefs")
                for ((key, value) in allPrefs) {
                    if (key.startsWith(SIGNATURE_KEY_PREFIX) && value is String && value.isNotEmpty()) {
                        signatureCache[key] = value
                        Log.d("加载的签名用于 $key")
                    }
                }
                Log.d("使用 ${signatureCache.size} 条目初始化的签名缓存")
            } catch (e: Exception) {
                Log.e("初始化签名缓存时出错：${e.message}")
            }
        }

        /**
         * 生成签名缓存的key
         */
        private fun getSignatureKey(packageName: String): String {
            return SIGNATURE_KEY_PREFIX + packageName
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 如果包名是QQ或者微信，则进行hook
        if (lpparam.packageName == "com.tencent.mobileqq" || lpparam.packageName == "com.tencent.mm" || lpparam.packageName == "com.tencent.tim") {

            Instrumentation::class.java.hookBeforeMethod(
                "callApplicationOnCreate", Application::class.java
            ) { param ->
                val application = param.args[0].safeCast<Application>() ?: return@hookBeforeMethod

                // 初始化签名缓存
                initSignatureCache(application)

                startKoin {
                    androidContext(application)
                    lazyModules(appModule)
                }
                application.registerActivityLifecycleCallbacks(object :
                    Application.ActivityLifecycleCallbacks {

                    override fun onActivityResumed(activity: Activity) {
//                        Log.d("onActivityResumed: $activity")
                        if (Utils.classNameList.any { activity.toString().contains(it) }) {
                            if (HostSettings.isLogin) {
                                checkDialogStatus(activity)
                            } else {
                                showLoginDialog(activity)
                            }
                        }
                    }

                    override fun onActivityDestroyed(activity: Activity) {
                    }

                    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
                        // 打印所有传参
//                        Log.d("onActivityCreated: $activity, $bundle")
//                        val intent = activity.intent
//                        val extras = intent.extras
//                        if (extras != null) {
//                            Log.d("---------------------${activity.javaClass.name}---------------------")
//                            for (key in extras.keySet()) {
//                                Log.d("Extra: $key -> ${extras.get(key)}")
//                            }
//                            Log.d("---------------------${activity.javaClass.name}----------")
//                        }

                    }

                    override fun onActivityStarted(activity: Activity) {

                    }


                    override fun onActivityPaused(activity: Activity) {
                    }


                    override fun onActivityStopped(activity: Activity) {

                    }

                    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {

                    }

                })
//                Log.d("sPrefs: ${sPrefs.all}")
            }

            "android.app.ApplicationPackageManager".hookAfterMethod(
                lpparam.classLoader, "getPackageInfo", String::class.java, Int::class.java
            ) { param ->
                val packageInfo = param.result.safeCast<PackageInfo>() ?: return@hookAfterMethod
                val pkg = packageInfo.packageName
                try {
                    val signatureKey = getSignatureKey(pkg)

                    // 优先从缓存读取签名
                    var signatures = signatureCache[signatureKey]

                    // 如果缓存中没有，尝试从SharedPreferences读取
                    if (signatures.isNullOrBlank()) {
                        signatures = sPrefs.getString(signatureKey, "")
                    }

                    // 如果SharedPreferences中也没有，尝试从APK的config.json中读取
                    if (signatures.isNullOrBlank()) {
                        signatures =
                            readSignatureFromApkConfig(packageInfo.applicationInfo?.sourceDir, pkg)
                        // 如果从APK读取到了签名，保存到sPrefs和缓存中
                        if (!signatures.isNullOrBlank()) {
                            sPrefs.edit(true) { putString(signatureKey, signatures) }
                            signatureCache[signatureKey] = signatures
                        }
                    }

                    if (!signatures.isNullOrBlank()) {
                        packageInfo.setObjectField("signatures", arrayOf((Signature(signatures))))
//                        Log.d("packageName: ${lpparam.packageName}\npkg: $pkg\nflag: $flag\nsignatures: $signatures")
                    }
                } catch (e: Exception) {
                    Log.e(e)
                }
            }

        }

    }

    /**
     * 从APK文件的assets/lspatch/config.json中读取originalSignature
     */
    private fun readSignatureFromApkConfig(apkPath: String?, packageName: String): String? {
        if (apkPath.isNullOrEmpty() || !File(apkPath).exists()) {
            return null
        }

        return try {
            ZipFile(apkPath).use { zipFile ->
                val configEntry = zipFile.getEntry("assets/lspatch/config.json")
                if (configEntry != null) {
                    zipFile.getInputStream(configEntry).use { inputStream ->
                        val jsonContent = inputStream.bufferedReader().readText()
                        val jsonObject = JSONObject(jsonContent)
                        val originalSignature = jsonObject.optString("originalSignature", "")
                        if (originalSignature.isNotEmpty()) {
                            Log.d("从 $packageName - config.json读取到的签名: $originalSignature")
                            return originalSignature
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.e(e)
            null
        }
    }

}