package website.xihan.signhelper.util

import android.content.Context


object NativeBridge {

    init {
        try {
            System.loadLibrary("nativehook")
        } catch (e: Throwable) {
            Log.e("loadLibrary failed: $e")
        }
    }

    @JvmStatic
    external fun addRule(src: String?, dst: String?)

    @JvmStatic
    external fun initHooks(cacheDir: String)

    @JvmStatic
    external fun setLogEnabled(enabled: Boolean)

    fun addRedirectRule(src: String, dst: String) {
        Log.d("[NativeBridge] Adding rule: $src -> $dst")
        addRule(src, dst)
    }

    fun initialize(context: Context) {
        val cacheDir = context.cacheDir.absolutePath
        Log.d("[NativeBridge] Initializing with cache dir: $cacheDir")
        initHooks(cacheDir)
    }
}