package cn.xihan.sign.initializer

import android.content.Context
import androidx.startup.Initializer

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 19:33
 * @介绍 :
 */
class AppInitializer : Initializer<Unit> {

    override fun create(context: Context) {

    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}