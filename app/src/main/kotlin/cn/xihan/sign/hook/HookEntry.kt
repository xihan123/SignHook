package cn.xihan.sign.hook

import android.content.pm.PackageInfo
import android.content.pm.Signature
import cn.xihan.sign.BuildConfig
import cn.xihan.sign.utli.defaultScopeSet
import cn.xihan.sign.utli.loge
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.android.ApplicationPackageManagerClass
import com.highcapable.yukihookapi.hook.type.android.PackageInfoClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 18:10
 * @介绍 :
 */
@InjectYukiHookWithXposed
class HookEntry : IYukiHookXposedInit {

    override fun onInit() = YukiHookAPI.configs {
        YLog.Configs.apply {
            tag = "yuki"
            isEnable = BuildConfig.DEBUG
        }
    }

    override fun onHook() = YukiHookAPI.encase {

        val packageNameList by lazy {
            prefs.getStringSet(
                "packageNameList", defaultScopeSet
            )
        }

        val packageNames by lazy {
            prefs.getStringSet("packageNames", emptySet())
        }

        if (packageName in packageNameList) {
            loadApp(packageName) {
                ApplicationPackageManagerClass.method {
                    name = "getPackageInfo"
                    param(StringClass, IntType)
                    returnType = PackageInfoClass
                }.hook().after {
                    val packageInfo = result as? PackageInfo ?: return@after

                    if (packageNames.isEmpty()) {
                        "apkSignatureList is empty".loge()
                        return@after
                    }
                    runCatching {
                        packageNames.forEach { packageName ->
                            val forgedSignature = prefs.getString(packageName, "")
                            if (packageInfo.packageName == packageName && forgedSignature.isNotEmpty()) {
                                packageInfo.signatures =
                                    arrayOf(Signature(forgedSignature))
                            }
                        }
                    }

                    result = packageInfo
                }
            }
        }
    }
}