package cn.xihan.signhook.util

import android.content.Context
import android.content.SharedPreferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/27 15:47
 * @介绍 :
 */
object HostSettings : KoinComponent, ISharedPreferencesOwner {

    val context by inject<Context>()

    override val sharedPreferences: SharedPreferences
        get() = context.getSharedPreferences("host_config", Context.MODE_PRIVATE)

    var isLogin by boolean()

    var email by string()

    var password by string()


}