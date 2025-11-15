@file:Suppress("unused")

package website.xihan.signhelper.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import de.robv.android.xposed.XposedBridge
import website.xihan.signhelper.BuildConfig
import java.io.File
import android.util.Log as ALog


object Log {

    private const val TAG = "KVContentProvider"
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var toast: Toast? = null
    private lateinit var logFile: File


    @JvmStatic
    private fun doLog(f: (String, String) -> Unit, obj: Any?, toXposed: Boolean = false) {
        val str = if (obj is Throwable) Log.getStackTraceString(obj) else obj.toString()

        if (str.length > maxLength) {
            val chunkCount: Int = str.length / maxLength
            for (i in 0..chunkCount) {
                val max: Int = maxLength * (i + 1)
                if (max >= str.length) {
                    doLog(f, str.substring(maxLength * i))
                } else {
                    doLog(f, str.substring(maxLength * i, max))
                }
            }
        } else {
            f(TAG, str)
            if (toXposed) XposedBridge.log("$TAG : $str")
        }
    }

    @JvmStatic
    fun d(obj: Any?) {
        if (!BuildConfig.DEBUG) return
        doLog(ALog::d, obj)
    }

    @JvmStatic
    fun i(obj: Any?) {
        doLog(ALog::i, obj)
    }

    @JvmStatic
    fun e(obj: Any?) {
        doLog(ALog::e, obj, true)
    }

    @JvmStatic
    fun v(obj: Any?) {
        doLog(ALog::v, obj)
    }

    @JvmStatic
    fun w(obj: Any?) {
        doLog(ALog::w, obj)
    }

    private const val maxLength = 100
}

