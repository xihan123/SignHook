package website.xihan.signhelper.util

import android.content.SharedPreferences
import website.xihan.signhelper.util.Const.fakeSignatureEnabledKey
import website.xihan.signhelper.util.Const.fakeSignatureKey
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class SharedPreferencesOwner(
    override val sharedPreferences: SharedPreferences
) : ISharedPreferencesOwner

interface ISharedPreferencesOwner {
    val sharedPreferences: SharedPreferences

    fun int(defaultValue: Int = 0) = SharedPreferencesProperty(
        { sharedPreferences.getInt(it, defaultValue) },
        { sharedPreferences.edit().putInt(first, second).commit() })

    fun long(defaultValue: Long = 0L) = SharedPreferencesProperty(
        { sharedPreferences.getLong(it, defaultValue) },
        { sharedPreferences.edit().putLong(first, second).commit() })

    fun float(defaultValue: Float = 0f) = SharedPreferencesProperty(
        { sharedPreferences.getFloat(it, defaultValue) },
        { sharedPreferences.edit().putFloat(first, second).commit() })

    fun boolean(defaultValue: Boolean = false) = SharedPreferencesProperty(
        { sharedPreferences.getBoolean(it, defaultValue) },
        { sharedPreferences.edit().putBoolean(first, second).commit() })

    fun string(defaultValue: String = "") = SharedPreferencesProperty(
        { sharedPreferences.getString(it, defaultValue) ?: defaultValue },
        { sharedPreferences.edit().putString(first, second).commit() })

    fun stringSet(defaultValue: Set<String> = emptySet()) = SharedPreferencesProperty({
        sharedPreferences.getStringSet(it, defaultValue) ?: emptySet()
    }, { sharedPreferences.edit().putStringSet(first, second).commit() })

    fun remove(key: String) = sharedPreferences.edit().remove(key).commit()

    fun clear() = sharedPreferences.edit().clear().commit()

}

class SharedPreferencesProperty<V>(
    internal val decode: (String) -> V, internal val encode: Pair<String, V>.() -> Boolean
) : ReadWriteProperty<ISharedPreferencesOwner, V> {

    @Volatile
    private var value: V? = null
    private var lastName: String? = null

    override fun getValue(thisRef: ISharedPreferencesOwner, property: KProperty<*>): V =
        decode(property.name)


    override fun setValue(thisRef: ISharedPreferencesOwner, property: KProperty<*>, value: V) {
//        Log.d("SharedPreferencesProperty: ${property.name} = $value")
        this.value = value
        encode((property.name) to value)
//        //写入本地在子线程处理，单一线程保证了写入顺序
//        taskExecutor.execute {
//            encode((property.name) to value)
//        }
    }

    companion object {
        /** 单一线程 无界队列  保证任务按照提交顺序来执行 **/
        private val taskExecutor = Executors.newSingleThreadExecutor(ThreadFactory {
            val thread = Thread(it)
            thread.name = "SharedPreferencesProperty"
            return@ThreadFactory thread
        })
    }
}

@JvmInline
value class ReadOnlySharedPreferences(
    private val preferences: SharedPreferences
) {
    operator fun <T> get(key: String, default: T): T = when (default) {
        is Int -> preferences.getInt(key, default)
        is Long -> preferences.getLong(key, default)
        is Float -> preferences.getFloat(key, default)
        is Boolean -> preferences.getBoolean(key, default)
        is String -> preferences.getString(key, default) ?: default
        is Set<*> -> preferences.getStringSet(key, default.toStringSet(key)) ?: default.toStringSet(
            key
        )

        else -> error(
            "Unsupported SharedPreferences value type for key `$key`: ${
            default?.let { it::class.qualifiedName }
        }")
    }.let {
        @Suppress("UNCHECKED_CAST") it as T
    }

    fun stringOrNull(key: String): String? = preferences.getString(key, null)

    fun contains(key: String): Boolean = preferences.contains(key)

    private fun Set<*>.toStringSet(key: String): Set<String> {
        require(all { it is String }) {
            "SharedPreferences only supports Set<String> for key `$key`"
        }
        return filterIsInstance<String>().toSet()
    }
}


fun SharedPreferences.asReadOnly(): ReadOnlySharedPreferences = ReadOnlySharedPreferences(this)

data class FakeSignatureConfig(
    val value: String
)

fun ReadOnlySharedPreferences.enabledFakeSignature(
    packageName: String
): FakeSignatureConfig? {
    val enabled = this[fakeSignatureEnabledKey(packageName), false]
    if (!enabled) return null

    val value = stringOrNull(fakeSignatureKey(packageName))?.trim().orEmpty()
    return FakeSignatureConfig(value).takeIf { it.value.isNotEmpty() }
}

@JvmInline
value class WriteOnlySharedPreferences(
    private val preferences: SharedPreferences
) {
    operator fun set(key: String, value: Any?) {
        put(key, value)
    }

    fun put(key: String, value: Any?): Boolean = when (value) {
        null -> remove(key)
        is Int -> edit { putInt(key, value) }
        is Long -> edit { putLong(key, value) }
        is Float -> edit { putFloat(key, value) }
        is Boolean -> edit { putBoolean(key, value) }
        is String -> edit { putString(key, value) }
        is Set<*> -> edit { putStringSet(key, value.toStringSet(key)) }
        else -> error("Unsupported SharedPreferences value type for key `$key`: ${value::class.qualifiedName}")
    }

    fun remove(key: String): Boolean = edit { remove(key) }

    fun clear(): Boolean = edit { clear() }

    private inline fun edit(block: SharedPreferences.Editor.() -> Unit): Boolean =
        preferences.edit().apply(block).commit()

    private fun Set<*>.toStringSet(key: String): Set<String> {
        require(all { it is String }) {
            "SharedPreferences only supports Set<String> for key `$key`"
        }
        return filterIsInstance<String>().toSet()
    }
}

fun SharedPreferences.asWriteOnly(): WriteOnlySharedPreferences = WriteOnlySharedPreferences(this)
