package cn.xihan.signhook.util

import android.content.Context
import android.content.SharedPreferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/26 17:27
 * @介绍 :
 */
object ModuleSettings : KoinComponent, ISharedPreferencesOwner {

    override val sharedPreferences: SharedPreferences by inject()

    var isLogin by boolean()

    var email by string()

    var password by string()

}