package cn.xihan.signhook.util

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * @项目名 : QDReaderHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/12 00:17
 * @介绍 :
 */


open class SharedPreferencesOwner(
    override val sharedPreferences: SharedPreferences
) : ISharedPreferencesOwner

interface ISharedPreferencesOwner {
    val sharedPreferences: SharedPreferences

    fun int(defaultValue: Int = 0) =
        SharedPreferencesProperty({ sharedPreferences.getInt(it, defaultValue) },
            { sharedPreferences.edit().putInt(first, second).commit() })

    fun long(defaultValue: Long = 0L) =
        SharedPreferencesProperty({ sharedPreferences.getLong(it, defaultValue) },
            { sharedPreferences.edit().putLong(first, second).commit() })

    fun float(defaultValue: Float = 0f) =
        SharedPreferencesProperty({ sharedPreferences.getFloat(it, defaultValue) },
            { sharedPreferences.edit().putFloat(first, second).commit() })

    fun boolean(defaultValue: Boolean = false) =
        SharedPreferencesProperty({ sharedPreferences.getBoolean(it, defaultValue) },
            { sharedPreferences.edit().putBoolean(first, second).commit() })

    fun string(defaultValue: String = "") =
        SharedPreferencesProperty({ sharedPreferences.getString(it, defaultValue) ?: defaultValue },
            { sharedPreferences.edit().putString(first, second).commit() })

    fun stringSet(defaultValue: Set<String> = emptySet()) = SharedPreferencesProperty({
        sharedPreferences.getStringSet(it, defaultValue) ?: emptySet()
    },
        { sharedPreferences.edit().putStringSet(first, second).commit() })

    fun remove(key: String) = sharedPreferences.edit().remove(key).commit()

    fun clear() = sharedPreferences.edit().clear().commit()

}

class SharedPreferencesProperty<V>(
    internal val decode: (String) -> V, internal val encode: Pair<String, V>.() -> Boolean
) : ReadWriteProperty<ISharedPreferencesOwner, V> {

    @Volatile
    private var value: V? = null

    override fun getValue(thisRef: ISharedPreferencesOwner, property: KProperty<*>): V =
        decode(property.name)


    override fun setValue(thisRef: ISharedPreferencesOwner, property: KProperty<*>, value: V) {
        this.value = value
        encode((property.name) to value)
    }
}
