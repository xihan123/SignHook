@file:Suppress("unused")

package website.xihan.signhelper.util

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import website.xihan.signhelper.BuildConfig
import website.xihan.signhelper.util.Const.TAG
import android.util.Log as AndroidLog

private const val maxLength = 4000


object ALog {


    @JvmStatic
    internal fun doLog(f: (String, String) -> Unit, obj: Any?, toXposed: Boolean = false) {
        val str = if (obj is Throwable) AndroidLog.getStackTraceString(obj) else obj.toString()
        f(TAG, str)
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

    @JvmStatic
    fun json(obj: Any?) {
        if (!BuildConfig.DEBUG) return
        json(TAG, obj.toString())
    }


    internal fun d(tag: String, msg: String) = logChunks(AndroidLog.DEBUG, tag, msg)

    internal fun i(tag: String, msg: String) = logChunks(AndroidLog.INFO, tag, msg)

    internal fun e(tag: String, msg: String) = logChunks(AndroidLog.ERROR, tag, msg)

    internal fun v(tag: String, msg: String) = logChunks(AndroidLog.VERBOSE, tag, msg)

    internal fun w(tag: String, msg: String) = logChunks(AndroidLog.WARN, tag, msg)

    internal fun json(tag: String, json: String) {
        try {
            val prettyJson = formatJson(json)
            logChunks(android.util.Log.DEBUG, tag, prettyJson)
        } catch (e: Exception) {
            logChunks(android.util.Log.ERROR, tag, "Invalid JSON: ${e.message}\n$json")
        }
    }

    internal fun logChunks(priority: Int, tag: String, msg: String) {
        if (msg.isEmpty()) return
        var start = 0
        while (start < msg.length) {
            var end = start
            var byteLen = 0
            while (end < msg.length) {
                val charBytes = when {
                    msg[end].code <= 0x7F -> 1
                    msg[end].code <= 0x7FF -> 2
                    msg[end].code <= 0xFFFF -> 3
                    else -> 4
                }
                if (byteLen + charBytes > maxLength) break
                byteLen += charBytes
                end++
            }
            if (end == start) end++
            val chunk = msg.substring(start, end)
            when (priority) {
                AndroidLog.VERBOSE -> AndroidLog.v(tag, chunk)
                AndroidLog.DEBUG -> AndroidLog.d(tag, chunk)
                AndroidLog.INFO -> AndroidLog.i(tag, chunk)
                AndroidLog.WARN -> AndroidLog.w(tag, chunk)
                AndroidLog.ERROR -> AndroidLog.e(tag, chunk)
            }
            start = end
        }
    }

    internal fun formatJson(json: String): String {
        val trimmed = json.trim()
        if (trimmed.startsWith("{")) return JSONObject(json).toString(4)
        if (trimmed.startsWith("[")) return JSONArray(json).toString(4)
        val objStart = trimmed.indexOf('{')
        val arrStart = trimmed.indexOf('[')
        val jsonStart = when {
            objStart < 0 && arrStart < 0 -> return json
            objStart < 0 -> arrStart
            arrStart < 0 -> objStart
            else -> minOf(objStart, arrStart)
        }
        if (jsonStart <= 0) return json
        val prefix = trimmed.substring(0, jsonStart)
        val jsonPart = trimmed.substring(jsonStart)
        return try {
            prefix + "\n" + JSONObject(jsonPart).toString(4)
        } catch (_: JSONException) {
            try {
                prefix + "\n" + JSONArray(jsonPart).toString(4)
            } catch (_: JSONException) {
                json
            }
        }
    }
}

