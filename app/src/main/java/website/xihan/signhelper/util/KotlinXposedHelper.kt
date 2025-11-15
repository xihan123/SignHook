@file:Suppress("unused")

package website.xihan.signhelper.util

import android.content.res.XResources
import android.view.View
import dalvik.system.BaseDexClassLoader
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge.hookAllConstructors
import de.robv.android.xposed.XposedBridge.hookAllMethods
import de.robv.android.xposed.XposedBridge.hookMethod
import de.robv.android.xposed.XposedBridge.invokeOriginalMethod
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.callStaticMethod
import de.robv.android.xposed.XposedHelpers.findAndHookConstructor
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.XposedHelpers.findClassIfExists
import de.robv.android.xposed.XposedHelpers.findField
import de.robv.android.xposed.XposedHelpers.findFieldIfExists
import de.robv.android.xposed.XposedHelpers.findFirstFieldByExactType
import de.robv.android.xposed.XposedHelpers.getBooleanField
import de.robv.android.xposed.XposedHelpers.getIntField
import de.robv.android.xposed.XposedHelpers.getLongField
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.getStaticObjectField
import de.robv.android.xposed.XposedHelpers.newInstance
import de.robv.android.xposed.XposedHelpers.setBooleanField
import de.robv.android.xposed.XposedHelpers.setFloatField
import de.robv.android.xposed.XposedHelpers.setIntField
import de.robv.android.xposed.XposedHelpers.setLongField
import de.robv.android.xposed.XposedHelpers.setObjectField
import de.robv.android.xposed.XposedHelpers.setStaticObjectField
import de.robv.android.xposed.callbacks.XC_LayoutInflated
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Modifier
import java.util.Enumeration

typealias MethodHookParam = MethodHookParam
typealias Replacer = (MethodHookParam) -> Any?
typealias Hooker = (MethodHookParam) -> Unit

fun Class<*>.hookMethod(method: String?, vararg args: Any?) = try {
    findAndHookMethod(this, method, *args)
} catch (e: NoSuchMethodError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

fun Member.hookMethod(callback: XC_MethodHook) = try {
    hookMethod(this, callback)
} catch (e: Throwable) {
    Log.e(e)
    null
}

inline fun MethodHookParam.callHooker(crossinline hooker: Hooker) = try {
    hooker(this)
} catch (e: Throwable) {
    Log.e("Error occurred calling hooker on ${this.method}")
    Log.e(e)
}

inline fun MethodHookParam.callReplacer(crossinline replacer: Replacer) = try {
    replacer(this)
} catch (e: Throwable) {
    Log.e("Error occurred calling replacer on ${this.method}")
    Log.e(e)
    null
}

inline fun Member.replaceMethod(crossinline replacer: Replacer) =
    hookMethod(object : XC_MethodReplacement() {
        override fun replaceHookedMethod(param: MethodHookParam) = param.callReplacer(replacer)
    })

inline fun Member.hookAfterMethod(crossinline hooker: Hooker) =
    hookMethod(object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

inline fun Member.hookBeforeMethod(crossinline hooker: (MethodHookParam) -> Unit) =
    hookMethod(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

inline fun Class<*>.hookBeforeMethod(
    method: String?, vararg args: Any?, crossinline hooker: Hooker
) = hookMethod(method, *args, object : XC_MethodHook() {
    override fun beforeHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
})

inline fun Class<*>.hookAfterMethod(
    method: String?, vararg args: Any?, crossinline hooker: Hooker
) = hookMethod(method, *args, object : XC_MethodHook() {
    override fun afterHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
})

inline fun Class<*>.replaceMethod(
    method: String?, vararg args: Any?, crossinline replacer: Replacer
) = hookMethod(method, *args, object : XC_MethodReplacement() {
    override fun replaceHookedMethod(param: MethodHookParam) = param.callReplacer(replacer)
})

inline fun Class<*>.hookAfterMethodByParameterTypes(
    parameterTypes: Array<Class<*>>, crossinline hooker: Hooker
) = try {
    val methods = declaredMethods.filter { it.parameterTypes.contentEquals(parameterTypes) }
    methods.forEach { method ->
        hookMethod(method, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
        })
    }
} catch (e: NoSuchMethodError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun Class<*>.hookBeforeMethodByParameterTypes(
    parameterTypes: Array<Class<*>>, crossinline hooker: Hooker
) = try {
    val methods = declaredMethods.filter { it.parameterTypes.contentEquals(parameterTypes) }
    methods.forEach { method ->
        hookMethod(method, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
        })
    }
} catch (e: NoSuchMethodError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun Class<*>.hookBeforeMethodByParameterTypes(
    method: String?, parameterSize: Int, crossinline hooker: Hooker
) = try {
    val methods =
        declaredMethods.filter { it.name == method && it.parameterTypes.size == parameterSize }
    methods.forEach {
        hookMethod(it, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
        })
    }
} catch (e: NoSuchMethodError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun Class<*>.hookAfterMethodByParameterTypes(
    method: String?, parameterSize: Int, crossinline hooker: Hooker
) = try {
    val methods =
        declaredMethods.filter { it.name == method && it.parameterTypes.size == parameterSize }
    methods.forEach {
        hookMethod(it, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
        })
    }
} catch (e: NoSuchMethodError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun Class<*>.replaceMethodByParameterTypes(
    parameterTypes: Array<Class<*>>, crossinline replacer: Replacer
) = try {
    val methods = declaredMethods.filter { it.parameterTypes.contentEquals(parameterTypes) }
    methods.forEach { method ->
        hookMethod(method, object : XC_MethodReplacement() {
            override fun replaceHookedMethod(param: MethodHookParam) = param.callReplacer(replacer)
        })
    }
} catch (e: NoSuchMethodError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun Class<*>.hookAfterMethodByParameterTypes(
    parameterTypeNames: Array<String>, crossinline hooker: Hooker
) = try {
    val methods = declaredMethods.filter { method ->
        method.parameters.map { it.type.name }.toTypedArray().contentEquals(parameterTypeNames)
    }
    methods.forEach { method ->
        hookMethod(method, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
        })
    }
} catch (e: NoSuchMethodError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun Class<*>.hookAfterMethodByParameterTypes(
    parameterSize: Int, crossinline hooker: Hooker
) = try {
    val methods = declaredMethods.filter { method -> method.parameterTypes.size == parameterSize }
    methods.forEach { method ->
        hookMethod(method, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
        })
    }
} catch (e: NoSuchMethodError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun Class<*>.hookBeforeMethodByParameterTypes(
    parameterTypeNames: Array<String>, crossinline hooker: Hooker
) = try {
    val methods = declaredMethods.filter { method ->
        method.parameters.map { it.type.name }.toTypedArray().contentEquals(parameterTypeNames)
    }
    methods.forEach { method ->
        hookMethod(method, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
        })
    }
} catch (e: NoSuchMethodError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun Class<*>.replaceMethodByParameterTypes(
    parameterTypeNames: Array<String>, crossinline replacer: Replacer
) = try {
    val methods = declaredMethods.filter { method ->
        method.parameters.map { it.type.name }.toTypedArray().contentEquals(parameterTypeNames)
    }
//    Log.d("class: $this, methods: $methods")
    methods.forEach { method ->
        hookMethod(method, object : XC_MethodReplacement() {
            override fun replaceHookedMethod(param: MethodHookParam) = param.callReplacer(replacer)
        })
    }
} catch (e: NoSuchMethodError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun Class<*>.replaceMethodByParameterSize(
    parameterSize: Int, returnType: Class<*>? = Unit::class.java, crossinline replacer: Replacer
) = try {
    val methods =
        declaredMethods.filter { it.parameterTypes.size == parameterSize && it.returnType == returnType }
    methods.forEach {
        hookMethod(it, object : XC_MethodReplacement() {
            override fun replaceHookedMethod(param: MethodHookParam) = param.callReplacer(replacer)
        })
    }
} catch (e: NoSuchMethodError) {
    Log.e(e)
} catch (e: ClassNotFoundError) {
    Log.e(e)
} catch (e: ClassNotFoundException) {
    Log.e(e)
}

inline fun Class<*>.replaceMethodByParameterSize(
    method: String, parameterSize: Int, crossinline replacer: Replacer
) = try {
    val methods =
        declaredMethods.filter { it.name == method && it.parameterTypes.size == parameterSize }
    methods.forEach {
        hookMethod(it, object : XC_MethodReplacement() {
            override fun replaceHookedMethod(param: MethodHookParam) = param.callReplacer(replacer)
        })
    }
} catch (e: NoSuchMethodError) {
    Log.e(e)
} catch (e: ClassNotFoundError) {
    Log.e(e)
} catch (e: ClassNotFoundException) {
    Log.e(e)
}


fun Class<*>.hookAllMethods(methodName: String?, hooker: XC_MethodHook): Set<XC_MethodHook.Unhook> =
    try {
        hookAllMethods(this, methodName, hooker)
    } catch (e: NoSuchMethodError) {
        Log.e(e)
        emptySet()
    } catch (e: ClassNotFoundError) {
        Log.e(e)
        emptySet()
    } catch (e: ClassNotFoundException) {
        Log.e(e)
        emptySet()
    }

inline fun Class<*>.hookBeforeAllMethods(methodName: String?, crossinline hooker: Hooker) =
    hookAllMethods(methodName, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

inline fun Class<*>.hookAfterAllMethods(methodName: String?, crossinline hooker: Hooker) =
    hookAllMethods(methodName, object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) = param.callHooker(hooker)

    })

inline fun Class<*>.replaceAllMethods(methodName: String?, crossinline replacer: Replacer) =
    hookAllMethods(methodName, object : XC_MethodReplacement() {
        override fun replaceHookedMethod(param: MethodHookParam) = param.callReplacer(replacer)
    })

fun Class<*>.hookConstructor(vararg args: Any?) = try {
    findAndHookConstructor(this, *args)
} catch (e: NoSuchMethodError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun Class<*>.hookBeforeConstructor(vararg args: Any?, crossinline hooker: Hooker) =
    hookConstructor(*args, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

inline fun Class<*>.hookAfterConstructor(vararg args: Any?, crossinline hooker: Hooker) =
    hookConstructor(*args, object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

inline fun Class<*>.replaceConstructor(vararg args: Any?, crossinline hooker: Hooker) =
    hookConstructor(*args, object : XC_MethodReplacement() {
        override fun replaceHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

fun Class<*>.hookAllConstructors(hooker: XC_MethodHook): Set<XC_MethodHook.Unhook> = try {
    hookAllConstructors(this, hooker)
} catch (e: NoSuchMethodError) {
    Log.e(e)
    emptySet()
} catch (e: ClassNotFoundError) {
    Log.e(e)
    emptySet()
} catch (e: ClassNotFoundException) {
    Log.e(e)
    emptySet()
}

inline fun Class<*>.hookAfterAllConstructors(crossinline hooker: Hooker) =
    hookAllConstructors(object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

inline fun Class<*>.hookBeforeAllConstructors(crossinline hooker: Hooker) =
    hookAllConstructors(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

inline fun Class<*>.replaceAllConstructors(crossinline hooker: Hooker) =
    hookAllConstructors(object : XC_MethodReplacement() {
        override fun replaceHookedMethod(param: MethodHookParam) = param.callHooker(hooker)
    })

fun String.hookMethod(classLoader: ClassLoader, method: String?, vararg args: Any?) = try {
    findClass(classLoader).hookMethod(method, *args)
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun String.hookBeforeMethod(
    classLoader: ClassLoader, method: String?, vararg args: Any?, crossinline hooker: Hooker
) = try {
    findClass(classLoader).hookBeforeMethod(method, *args, hooker = hooker)
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun String.hookAfterMethod(
    classLoader: ClassLoader, method: String?, vararg args: Any?, crossinline hooker: Hooker
) = try {
    findClass(classLoader).hookAfterMethod(method, *args, hooker = hooker)
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

inline fun String.replaceMethod(
    classLoader: ClassLoader, method: String?, vararg args: Any?, crossinline replacer: Replacer
) = try {
    findClass(classLoader).replaceMethod(method, *args, replacer = replacer)
} catch (e: ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

fun MethodHookParam.invokeOriginalMethod(): Any? = invokeOriginalMethod(method, thisObject, args)

inline fun <T, R> T.runCatchingOrNull(func: T.() -> R?) = try {
    func()
} catch (e: Throwable) {
    null
}

fun Any.getObjectField(field: String?): Any? = getObjectField(this, field)

fun Any.getObjectFieldOrNull(field: String?): Any? = runCatchingOrNull {
    getObjectField(this, field)
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.getObjectFieldAs(field: String?) = getObjectField(this, field) as T

@Suppress("UNCHECKED_CAST")
fun <T> Any.getObjectFieldOrNullAs(field: String?) = runCatchingOrNull {
    getObjectField(this, field) as T
}

fun Any.getIntField(field: String?) = getIntField(this, field)

fun Any.getIntFieldOrNull(field: String?) = runCatchingOrNull {
    getIntField(this, field)
}

fun Any.getLongField(field: String?) = getLongField(this, field)

fun Any.getLongFieldOrNull(field: String?) = runCatchingOrNull {
    getLongField(this, field)
}

fun Any.getBooleanFieldOrNull(field: String?) = runCatchingOrNull {
    getBooleanField(this, field)
}

fun Any.callMethod(methodName: String?, vararg args: Any?): Any? =
    callMethod(this, methodName, *args)

fun Any.callMethodOrNull(methodName: String?, vararg args: Any?): Any? = runCatchingOrNull {
    callMethod(this, methodName, *args)
}

fun Class<*>.callStaticMethod(methodName: String?, vararg args: Any?): Any? =
    callStaticMethod(this, methodName, *args)

fun Class<*>.callStaticMethodOrNull(methodName: String?, vararg args: Any?): Any? =
    runCatchingOrNull {
        callStaticMethod(this, methodName, *args)
    }

@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.callStaticMethodAs(methodName: String?, vararg args: Any?) =
    callStaticMethod(this, methodName, *args) as T

@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.callStaticMethodOrNullAs(methodName: String?, vararg args: Any?) =
    runCatchingOrNull {
        callStaticMethod(this, methodName, *args) as T
    }

@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.getStaticObjectFieldAs(field: String?) = getStaticObjectField(this, field) as T

@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.getStaticObjectFieldOrNullAs(field: String?) = runCatchingOrNull {
    getStaticObjectField(this, field) as T
}

fun Class<*>.getStaticObjectField(field: String?): Any? = getStaticObjectField(this, field)

fun Class<*>.getStaticObjectFieldOrNull(field: String?): Any? = runCatchingOrNull {
    getStaticObjectField(this, field)
}

fun Class<*>.setStaticObjectField(field: String?, obj: Any?) = apply {
    setStaticObjectField(this, field, obj)
}

fun Class<*>.setStaticObjectFieldIfExist(field: String?, obj: Any?) = apply {
    try {
        setStaticObjectField(this, field, obj)
    } catch (ignored: Throwable) {
    }
}

inline fun <reified T> Class<*>.findFieldByExactType(): Field? =
    findFirstFieldByExactType(this, T::class.java)

fun Class<*>.findFieldByExactType(type: Class<*>): Field? = findFirstFieldByExactType(this, type)

fun Any.getViews(type: Class<*>, isSuperClass: Boolean = false): ArrayList<Any> {
    val list = arrayListOf<Any>()
    var clazz: Class<*> = this.javaClass
    do {
        clazz.declaredFields.forEach { field ->
            runCatching {
                field.isAccessible = true
                field.get(this)?.takeIf { type.isInstance(it) }?.let {
                    list.add(it)
                }
            }.onFailure {
                Log.e("Error getting field ${field.name} in ${clazz.name}, error: ${it.message}")
            }
        }
        clazz = clazz.superclass
    } while (isSuperClass && clazz != Any::class.java)
    return list
}

@Throws(NoSuchFieldException::class, IllegalAccessException::class)
inline fun <reified T : View> Any.getViews(isSuperClass: Boolean = false) =
    getParamList<T>(isSuperClass)

@Throws(NoSuchFieldException::class, IllegalAccessException::class)
inline fun <reified T> Any.getParamList(isSuperClass: Boolean = false): ArrayList<T> {
    // 定义一个泛型函数getParamList，接收一个布尔参数isSuperClass，默认值为false，返回一个ArrayList<T>
    val list = arrayListOf<T>()
    // 初始化一个空的ArrayList<T>用于存储结果
    var clazz: Class<*> = this.javaClass
    // 获取当前对象的类类型
    do {
        clazz.declaredFields.forEach { field ->
            // 遍历当前类声明的所有字段
            runCatching {
                // 使用runCatching来捕获可能的异常
                field.isAccessible = true
                // 设置字段为可访问，即使它是私有的
                field.get(this)?.let { value ->
                    // 获取字段的值，如果值不为null，则执行let函数
                    if (T::class.java.isInstance(value)) {
                        // 检查字段的值是否是泛型T的实例
                        list.add(value as T)
                        // 将值强制转换为T类型并添加到结果列表中
                    }
                }
            }.onFailure {
                // 如果捕获到异常，则输出错误日志
                Log.e("Error getting param ${field.name} in ${clazz.simpleName}, error: ${it.message}")
                // 输出错误信息，包括字段名、类名和异常信息
            }
        }
        clazz = clazz.superclass ?: break
        // 获取当前类的父类，如果没有父类则退出循环
    } while (isSuperClass && clazz != Any::class.java)
    // 如果isSuperClass为true且当前类不是Any类，则继续循环
    return list
    // 返回结果列表
}


@Suppress("UNCHECKED_CAST")
fun <T> Any.callMethodAs(methodName: String?, vararg args: Any?) =
    callMethod(this, methodName, *args) as T

@Suppress("UNCHECKED_CAST")
fun <T> Any.callMethodOrNullAs(methodName: String?, vararg args: Any?) = runCatchingOrNull {
    callMethod(this, methodName, *args) as T
}

fun Any.callMethod(methodName: String?, parameterTypes: Array<Class<*>>, vararg args: Any?): Any? =
    callMethod(this, methodName, parameterTypes, *args)

fun Any.callMethodOrNull(
    methodName: String?, parameterTypes: Array<Class<*>>, vararg args: Any?
): Any? = runCatchingOrNull {
    callMethod(this, methodName, parameterTypes, *args)
}

fun Class<*>.callStaticMethod(
    methodName: String?, parameterTypes: Array<Class<*>>, vararg args: Any?
): Any? = callStaticMethod(this, methodName, parameterTypes, *args)

fun Class<*>.callStaticMethodOrNull(
    methodName: String?, parameterTypes: Array<Class<*>>, vararg args: Any?
): Any? = runCatchingOrNull {
    callStaticMethod(this, methodName, parameterTypes, *args)
}

fun String.findClass(classLoader: ClassLoader?): Class<*> = findClass(this, classLoader)

infix fun String.on(classLoader: ClassLoader?): Class<*> = findClass(this, classLoader)

fun String.findClassOrNull(classLoader: ClassLoader?): Class<*>? =
    findClassIfExists(this, classLoader)

infix fun String.from(classLoader: ClassLoader?): Class<*>? = findClassIfExists(this, classLoader)

fun Class<*>.new(vararg args: Any?): Any = newInstance(this, *args)

fun Class<*>.new(parameterTypes: Array<Class<*>>, vararg args: Any?): Any =
    newInstance(this, parameterTypes, *args)

fun Class<*>.findField(field: String?): Field = findField(this, field)

fun Class<*>.findFieldOrNull(field: String?): Field? = findFieldIfExists(this, field)

fun <T> T.setIntField(field: String?, value: Int) = apply {
    setIntField(this, field, value)
}

fun <T> T.setLongField(field: String?, value: Long) = apply {
    setLongField(this, field, value)
}

fun <T> T.setObjectField(field: String?, value: Any?) = apply {
    setObjectField(this, field, value)
}

fun <T> T.setBooleanField(field: String?, value: Boolean) = apply {
    setBooleanField(this, field, value)
}

fun <T> T.setFloatField(field: String?, value: Float) = apply {
    setFloatField(this, field, value)
}

inline fun XResources.hookLayout(
    id: Int, crossinline hooker: (XC_LayoutInflated.LayoutInflatedParam) -> Unit
) {
    try {
        hookLayout(id, object : XC_LayoutInflated() {
            override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                try {
                    hooker(liparam)
                } catch (e: Throwable) {
                    Log.e(e)
                }
            }
        })
    } catch (e: Throwable) {
        Log.e(e)
    }
}

inline fun XResources.hookLayout(
    pkg: String,
    type: String,
    name: String,
    crossinline hooker: (XC_LayoutInflated.LayoutInflatedParam) -> Unit
) {
    try {
        val id = getIdentifier(name, type, pkg)
        hookLayout(id, hooker)
    } catch (e: Throwable) {
        Log.e(e)
    }
}

fun Class<*>.findFirstFieldByExactType(type: Class<*>): Field =
    findFirstFieldByExactType(this, type)

fun Class<*>.findFirstFieldByExactTypeOrNull(type: Class<*>?): Field? = runCatchingOrNull {
    findFirstFieldByExactType(this, type)
}

fun Any.getFirstFieldByExactType(type: Class<*>): Any? =
    javaClass.findFirstFieldByExactType(type).get(this)

@Suppress("UNCHECKED_CAST")
fun <T> Any.getFirstFieldByExactTypeAs(type: Class<*>) =
    javaClass.findFirstFieldByExactType(type).get(this) as? T

inline fun <reified T : Any> Any.getFirstFieldByExactType() =
    javaClass.findFirstFieldByExactType(T::class.java).get(this) as? T

fun Any.getFirstFieldByExactTypeOrNull(type: Class<*>?): Any? = runCatchingOrNull {
    javaClass.findFirstFieldByExactTypeOrNull(type)?.get(this)
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.getFirstFieldByExactTypeOrNullAs(type: Class<*>?) =
    getFirstFieldByExactTypeOrNull(type) as? T

inline fun <reified T> Any.getFirstFieldByExactTypeOrNull() =
    getFirstFieldByExactTypeOrNull(T::class.java) as? T

inline fun ClassLoader.findDexClassLoader(crossinline delegator: (BaseDexClassLoader) -> BaseDexClassLoader = { x -> x }): BaseDexClassLoader? {
    var classLoader = this
    while (classLoader !is BaseDexClassLoader) {
        if (classLoader.parent != null) classLoader = classLoader.parent
        else return null
    }
    return delegator(classLoader)
}

inline fun ClassLoader.allClassesList(crossinline delegator: (BaseDexClassLoader) -> BaseDexClassLoader = { x -> x }): List<String> {
    return findDexClassLoader(delegator)?.getObjectField("pathList")
        ?.getObjectFieldAs<Array<Any>>("dexElements")?.flatMap {
            it.getObjectField("dexFile")?.callMethodAs<Enumeration<String>>("entries")?.toList()
                .orEmpty()
        }.orEmpty()
}

val Member.isStatic: Boolean
    inline get() = Modifier.isStatic(modifiers)
val Member.isFinal: Boolean
    inline get() = Modifier.isFinal(modifiers)
val Member.isPublic: Boolean
    inline get() = Modifier.isPublic(modifiers)
val Member.isNotStatic: Boolean
    inline get() = !isStatic
val Member.isAbstract: Boolean
    inline get() = Modifier.isAbstract(modifiers)
val Member.isPrivate: Boolean
    inline get() = Modifier.isPrivate(modifiers)
val Class<*>.isAbstract: Boolean
    inline get() = !isPrimitive && Modifier.isAbstract(modifiers)
val Class<*>.isFinal: Boolean
    inline get() = !isPrimitive && Modifier.isFinal(modifiers)


fun ClassLoader.returnFalse(className: String, methodName: String) {
    className.from(this)?.replaceMethod(methodName) { false }
}

fun ClassLoader.returnTrue(className: String, methodName: String) {
    className.from(this)?.replaceMethod(methodName) { true }
}

inline fun ClassLoader.intercept(
    className: String, methodName: String, crossinline interceptor: (MethodHookParam) -> Any? = {}
) {
    className.from(this)?.replaceAllMethods(methodName, interceptor)
}

inline fun ClassLoader.intercept(
    className: String,
    methodName: String,
    parameterSize: Int,
    crossinline interceptor: (MethodHookParam) -> Any? = {}
) {
    className.from(this)?.replaceMethodByParameterSize(methodName, parameterSize, interceptor)
}

inline fun ClassLoader.intercept(
    className: String,
    parameterSize: Int,
    returnType: Class<*>? = Unit::class.java,
    crossinline interceptor: (MethodHookParam) -> Any? = {}
) {
    className.from(this)?.replaceMethodByParameterSize(parameterSize, returnType, interceptor)
}

inline fun Class<*>.intercept(
    methodName: String, crossinline interceptor: (MethodHookParam) -> Any? = {}
) {
    replaceAllMethods(methodName, interceptor)
}

inline fun Class<*>.intercept(
    methodName: String, parameterSize: Int, crossinline interceptor: (MethodHookParam) -> Any? = {}
) {
    replaceMethodByParameterSize(methodName, parameterSize, interceptor)
}

inline fun Class<*>.intercept(
    parameterTypes: Array<String>, crossinline interceptor: (MethodHookParam) -> Any? = {}
) {
    replaceMethodByParameterTypes(parameterTypes, interceptor)
}

inline fun Class<*>.intercept(
    parameterSize: Int,
    returnType: Class<*>? = Unit::class.java,
    crossinline interceptor: (MethodHookParam) -> Any? = {}
) {
    replaceMethodByParameterSize(parameterSize, returnType, interceptor)
}

/**
 * 传入类名打印所以函数
 */
fun ClassLoader.printAllMethods(className: String, isCallStackTrace: Boolean = false) {
    try {
        className.from(this)?.apply {
            declaredMethods.forEach {
                hookAfterAllMethods(it.name) { param ->
                    val logs = buildString {
                        append("Method: ${param.method.name}, Args: ")
                        param.args.forEachIndexed { index, arg ->
                            append("Arg$index: ${arg.toStringOrEmpty() ?: arg}")
                        }
                        append("Result: ${param.result.toStringOrEmpty() ?: param.result}")

                        if (isCallStackTrace) {
                            it.name.throwRuntimeException()
                        }
                    }
                    Log.d(logs)
//                    Log.d("${param.method.name}: ${param.args.joinToString(", ")} result: ${param.result}")
                }
            }
        }
    } catch (e: Exception) {
        Log.e(e)
    }
}

/**
 * 抛出一个运行时异常
 */
fun String.throwRuntimeException() {
    try {
        throw RuntimeException()
    } catch (e: Exception) {
        Log.e("method: $this\ncallStackTrace: ${e.stackTrace.joinToString("\n")}")
    }
}

/**
 * Any 拓展函数获取字符串
 */
fun Any.toStringOrEmpty(): String? = runCatching {
    when (this) {
        is String -> this
        is Int -> this.toString()
        is Float -> this.toString()
        is Double -> this.toString()
        is Boolean -> this.toString()
        is ByteArray -> String(this)
        else -> toString()
    }
}.getOrNull()

