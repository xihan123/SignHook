package cn.xihan.signhook

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.os.Bundle
import cn.xihan.signhook.util.HostSettings
import cn.xihan.signhook.util.Log
import cn.xihan.signhook.util.Utils
import cn.xihan.signhook.util.Utils.checkDialogStatus
import cn.xihan.signhook.util.Utils.showLoginDialog
import cn.xihan.signhook.util.appModule
import cn.xihan.signhook.util.hookAfterMethod
import cn.xihan.signhook.util.hookBeforeMethod
import cn.xihan.signhook.util.sPrefs
import cn.xihan.signhook.util.safeCast
import cn.xihan.signhook.util.setObjectField
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.lazyModules

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/21 16:21
 * @介绍 :
 */
class HookEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 如果包名是QQ或者微信，则进行hook
        if (lpparam.packageName == "com.tencent.mobileqq" || lpparam.packageName == "com.tencent.mm" || lpparam.packageName == "com.tencent.tim") {

            Instrumentation::class.java.hookBeforeMethod(
                "callApplicationOnCreate", Application::class.java
            ) { param ->
                val application = param.args[0].safeCast<Application>() ?: return@hookBeforeMethod
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
                lpparam.classLoader,
                "getPackageInfo",
                String::class.java,
                Int::class.java
            ) { param ->
                val packageInfo = param.result.safeCast<PackageInfo>() ?: return@hookAfterMethod
                val pkg = packageInfo.packageName
                try {
                    // 尝试读取签名
                    val signatures = sPrefs.getString(pkg, "")
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


}