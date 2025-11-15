package website.xihan.signhelper.util

import website.xihan.kv.IKVOwner
import website.xihan.kv.KVOwner

object ModuleConfig : IKVOwner by KVOwner("SHARED_SETTINGS") {

    /**
     * 作用域
     */
    var scopes by kvStringSet(
        setOf(
            "com.tencent.mobileqq",
            "com.tencent.tim",
            "com.tencent.mm",
            "com.ss.android.ugc.aweme"
        )
    )


}