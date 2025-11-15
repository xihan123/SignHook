package website.xihan.signhelper.hook

import android.app.Application
import android.app.Instrumentation
import android.content.pm.PackageInfo
import android.content.pm.Signature
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import website.xihan.kv.HostKVManager
import website.xihan.kv.KVFileReceiver
import website.xihan.signhelper.BuildConfig
import website.xihan.signhelper.util.Const.FAKE_SIGNATURE
import website.xihan.signhelper.util.Const.REDIRECT
import website.xihan.signhelper.util.Const.REDIRECT_PATH
import website.xihan.signhelper.util.Const.REDIRECT_STATUS
import website.xihan.signhelper.util.Log
import website.xihan.signhelper.util.NativeBridge
import website.xihan.signhelper.util.deleteFile
import website.xihan.signhelper.util.hookAfterMethod
import website.xihan.signhelper.util.safeCast
import website.xihan.signhelper.util.setObjectField

class HookEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        Instrumentation::class.java.hookAfterMethod(
            "callApplicationOnCreate", Application::class.java
        ) { param ->
            val application = param.args[0].safeCast<Application>() ?: return@hookAfterMethod
            startKoin {
                androidContext(application)
                androidLogger()
            }
            HostKVManager.init(
                enableSharedPreferencesCache = true, modulePackageName = BuildConfig.APPLICATION_ID
            )

            runCatching {
//                Log.d("Setup file receiver\npackageName：${lpparam.packageName}")
                if (lpparam.packageName == BuildConfig.APPLICATION_ID) return@runCatching
                val receiveFile = KVFileReceiver(application)
                val redirectPath = HostKVManager.createKVHelper(REDIRECT_PATH)
                receiveFile.receiveFile(REDIRECT, lpparam.packageName)?.let { file ->
                    redirectPath.getString(lpparam.packageName).deleteFile()
                    redirectPath.putString(
                        key = lpparam.packageName, value = file.absolutePath
                    )
//                    Log.d("${lpparam.packageName} File saved to: ${file.absolutePath}")
                }
                receiveFile.observeFile(REDIRECT, lpparam.packageName) { file ->
                    redirectPath.getString(lpparam.packageName).deleteFile()

                    redirectPath.putString(
                        key = lpparam.packageName, value = file.absolutePath
                    )
//                    Log.d("${lpparam.packageName} File updated and saved to: ${file.absolutePath}")
                }
            }.onFailure {
                Log.e("Failed to setup file receiver")
            }


            runCatching {
                if (lpparam.packageName == BuildConfig.APPLICATION_ID) return@runCatching
                val enableRedirect =
                    HostKVManager.createKVHelper(REDIRECT_STATUS).getBoolean(lpparam.packageName)
//                Log.d("Redirect status: $enableRedirect")
                if (!enableRedirect) return@runCatching
                val redirectPath =
                    HostKVManager.createKVHelper(REDIRECT_PATH).getString(lpparam.packageName)
//                Log.d("Redirect path: $redirectPath")
                if (redirectPath.isBlank()) return@runCatching
                NativeBridge.enablePathRedirect(
                    application.applicationInfo.sourceDir, redirectPath, true
                )
            }.onFailure {
                Log.e("Failed to setup redirect path")
            }

            "android.app.ApplicationPackageManager".hookAfterMethod(
                lpparam.classLoader, "getPackageInfo", String::class.java, Int::class.java
            ) { param ->
                val packageInfo = param.result.safeCast<PackageInfo>() ?: return@hookAfterMethod
                val pkg = packageInfo.packageName
                try {
                    val signatures = HostKVManager.createKVHelper(FAKE_SIGNATURE).getString(pkg)

                    if (signatures.isNotBlank()) {
                        packageInfo.setObjectField(
                            "signatures", arrayOf((Signature(signatures)))
                        )
//                        Log.d("packageName: ${lpparam.packageName}\npkg: $pkg\nsignatures: $signatures")
                    }
                } catch (e: Exception) {
                    Log.e(e)
                }
            }


        }

    }
}