package website.xihan.signhelper.util

object Const {

    const val REMOTE_PREFS_GROUP = ""

    // 伪装签名
    const val FAKE_SIGNATURE = "fakeSignature"

    const val FAKE_SIGNATURE_ENABLED = "fakeSignatureEnabled"

    const val PREFS_DYNAMIC_KEY_SEPARATOR = "\$\$"

    const val TAG = "SignHelper"

    fun fakeSignatureKey(packageName: String): String =
        FAKE_SIGNATURE + PREFS_DYNAMIC_KEY_SEPARATOR + packageName

    fun fakeSignatureEnabledKey(packageName: String): String =
        FAKE_SIGNATURE_ENABLED + PREFS_DYNAMIC_KEY_SEPARATOR + packageName

}
