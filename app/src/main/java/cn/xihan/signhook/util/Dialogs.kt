@file:Suppress("unused")

package cn.xihan.signhook.util

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import android.view.View
import androidx.annotation.StringRes
import kotlin.DeprecationLevel.ERROR

internal const val NO_GETTER: String = "Property does not have a getter"

internal fun noGetter(): Nothing = throw NotImplementedError(NO_GETTER)

/**
 * 警报生成器工厂
 * @suppress Generate Documentation
 */
typealias AlertBuilderFactory<D> = (Context) -> AlertBuilder<D>

/**
 * 应用程序兼容
 * @suppress Generate Documentation
 */
val AppCompat: AlertBuilderFactory<DialogInterface> = { context ->
    object : AlertDialogBuilder() {
        override val builder: AlertDialog.Builder = AlertDialog.Builder(context)
    }
}

/**
 * 警觉
 * @since 7.9.374-1416
 * @param [message] 消息
 * @param [title] 标题
 * @param [block] 块
 * @suppress Generate Documentation
 */
fun Context.alert(
    message: CharSequence,
    title: CharSequence? = null,
    block: (AlertBuilder<*>.() -> Unit)? = null,
) = alert(AppCompat, message, title, block)

/**
 * 警觉
 * @since 7.9.374-1416
 * @param [factory] 工厂
 * @param [message] 消息
 * @param [title] 标题
 * @param [block] 块
 * @suppress Generate Documentation
 */
inline fun <D : DialogInterface> Context.alert(
    factory: AlertBuilderFactory<D>,
    message: CharSequence,
    title: CharSequence? = null,
    noinline block: (AlertBuilder<D>.() -> Unit)? = null,
) = alertDialog(factory) {
    title?.let { this.title = it }
    this.message = message
    block?.invoke(this)
}.show()

/**
 * 警报对话框
 * @since 7.9.374-1416
 * @param [block] 块
 * @suppress Generate Documentation
 */
fun Context.alertDialog(block: AlertBuilder<*>.() -> Unit) = alertDialog(AppCompat, block)

/**
 * 警报对话框
 * @since 7.9.374-1416
 * @param [factory] 工厂
 * @param [block] 块
 * @suppress Generate Documentation
 */
inline fun <D : DialogInterface> Context.alertDialog(
    factory: AlertBuilderFactory<D>,
    block: AlertBuilder<D>.() -> Unit,
) = factory(this).apply(block)

/**
 * 多选选择器
 * @since 7.9.374-1416
 * @param [items] 项目
 * @param [checkItems] 检查项目
 * @param [title] 标题
 * @param [onItemSelected] 在所选项目上
 * @suppress Generate Documentation
 */
fun Context.multiChoiceSelector(
    items: List<CharSequence>,
    checkItems: BooleanArray,
    title: CharSequence? = null,
    onItemSelected: (DialogInterface, Int, Boolean) -> Unit,
) = multiChoiceSelector(AppCompat, items, checkItems, title, onItemSelected)

/**
 * 单选选择器
 * @since 7.9.374-1416
 * @param [items] 项目
 * @param [checkIndex] 检查索引
 * @param [title] 标题
 * @param [onItemSelected] 在所选项目上
 * @suppress Generate Documentation
 */
fun <T> Context.singleChoiceSelector(
    items: List<T>,
    checkIndex: Int,
    title: CharSequence? = null,
    onItemSelected: (DialogInterface, T, Int) -> Unit
) = singleChoiceSelector(AppCompat, items, checkIndex, title, onItemSelected)

/**
 * 多选选择器
 * @since 7.9.374-1416
 * @param [factory] 工厂
 * @param [items] 项目
 * @param [checkItems] 检查项目
 * @param [title] 标题
 * @param [onItemSelected] 在所选项目上
 * @suppress Generate Documentation
 */
inline fun <D : DialogInterface> Context.multiChoiceSelector(
    factory: AlertBuilderFactory<D>,
    items: List<CharSequence>,
    checkItems: BooleanArray,
    title: CharSequence? = null,
    noinline onItemSelected: (DialogInterface, Int, Boolean) -> Unit,
) = alertDialog(factory) {
    title?.let { this.title = it }
    multiChoiceItems(items, checkItems, onItemSelected)
}.show()

/**
 * 单选选择器
 * @since 7.9.374-1416
 * @param [factory] 工厂
 * @param [items] 项目
 * @param [checkIndex] 检查索引
 * @param [title] 标题
 * @param [onItemSelected] 在所选项目上
 * @suppress Generate Documentation
 */
inline fun <D : DialogInterface, T> Context.singleChoiceSelector(
    factory: AlertBuilderFactory<D>,
    items: List<T>,
    checkIndex: Int,
    title: CharSequence? = null,
    noinline onItemSelected: (DialogInterface, T, Int) -> Unit
) = alertDialog(factory) {
    title?.let { this.title = it }
    singleChoiceItems(items, checkIndex, onItemSelected)
}.show()

/**
 * ok按钮
 * @since 7.9.374-1416
 * @param [onClicked] 单击时
 * @suppress Generate Documentation
 */
fun AlertBuilder<*>.okButton(onClicked: (dialog: DialogInterface) -> Unit) =
    positiveButton(android.R.string.ok, onClicked)

/**
 * 取消按钮
 * @since 7.9.374-1416
 * @param [onClicked] 单击时
 * @suppress Generate Documentation
 */
fun AlertBuilder<*>.cancelButton(onClicked: (dialog: DialogInterface) -> Unit = { it.dismiss() }) =
    negativeButton(android.R.string.cancel, onClicked)

/**
 * 单选项目
 * @since 7.9.374-1416
 * @param [items] 项目
 * @param [checkIndex] 检查索引
 * @param [onItemSelected] 在所选项目上
 * @suppress Generate Documentation
 */
inline fun <T> AlertBuilder<*>.singleChoiceItems(
    items: List<T>, checkIndex: Int, crossinline onItemSelected: (DialogInterface, T, Int) -> Unit
) = singleChoiceItems(items.map { it.toString() }, checkIndex) { dialog, which ->
    onItemSelected(dialog, items[which], which)
}

/**
 * 单选项目
 * @since 7.9.374-1416
 * @param [items] 项目
 * @param [checkItem] 检查项目
 * @param [onItemSelected] 在所选项目上
 * @suppress Generate Documentation
 */
inline fun AlertBuilder<*>.singleChoiceItems(
    items: List<CharSequence>,
    checkItem: CharSequence,
    crossinline onItemSelected: (DialogInterface, Int) -> Unit
) = singleChoiceItems(items.map { it.toString() },
    items.indexOfFirst { it == checkItem }) { dialog, which ->
    onItemSelected(dialog, which)
}

/**
 * 单选项目
 * @since 7.9.374-1416
 * @param [items] 项目
 * @param [checkItem] 检查项目
 * @param [onItemSelected] 在所选项目上
 * @suppress Generate Documentation
 */
inline fun <T> AlertBuilder<*>.singleChoiceItems(
    items: List<T>, checkItem: T, crossinline onItemSelected: (DialogInterface, T, Int) -> Unit
) = singleChoiceItems(items.map { it.toString() },
    items.indexOfFirst { it == checkItem }) { dialog, which ->
    onItemSelected(dialog, items[which], which)
}

/**
 * 多选项目
 * @since 7.9.374-1416
 * @param [items] 项目
 * @param [checkItems] 检查项目
 * @param [onItemSelected] 在所选项目上
 * @suppress Generate Documentation
 */
inline fun <T> AlertBuilder<*>.multiChoiceItems(
    items: List<T>,
    checkItems: BooleanArray,
    crossinline onItemSelected: (DialogInterface, T, Int, Boolean) -> Unit,
) = multiChoiceItems(items.map { it.toString() }, checkItems) { dialog, which, isChecked ->
    onItemSelected(dialog, items[which], which, isChecked)
}

/**
 * 取消时执行
 * @since 7.9.374-1416
 * @param [block] 块
 * @suppress Generate Documentation
 */
fun Dialog.doOnCancel(block: (DialogInterface) -> Unit) = apply {
    setOnCancelListener(block)
}

/**
 * 解雇时执行
 * @since 7.9.374-1416
 * @param [block] 块
 * @suppress Generate Documentation
 */
fun Dialog.doOnDismiss(block: (DialogInterface) -> Unit) = apply {
    setOnDismissListener(block)
}

/**
 * 表演中表演
 * @since 7.9.374-1416
 * @param [block] 块
 * @suppress Generate Documentation
 */
fun Dialog.doOnShow(block: (DialogInterface) -> Unit) = apply {
    setOnShowListener(block)
}

/**
 * 取消时执行
 * @since 7.9.374-1416
 * @param [block] 块
 * @suppress Generate Documentation
 */
fun DialogInterface.doOnCancel(block: (DialogInterface) -> Unit) = apply {
    check(this is Dialog)
    setOnCancelListener(block)
}

/**
 * 解雇时执行
 * @since 7.9.374-1416
 * @param [block] 块
 * @suppress Generate Documentation
 */
fun DialogInterface.doOnDismiss(block: (DialogInterface) -> Unit) = apply {
    check(this is Dialog)
    setOnDismissListener(block)
}

/**
 * 表演中表演
 * @since 7.9.374-1416
 * @param [block] 块
 * @suppress Generate Documentation
 */
fun DialogInterface.doOnShow(block: (DialogInterface) -> Unit) = apply {
    check(this is Dialog)
    setOnShowListener(block)
}

/**
 * 警报生成器
 * 创建[AlertBuilder]
 * @suppress Generate Documentation
 */
interface AlertBuilder<out D : DialogInterface> {
    val context: Context

    var title: CharSequence
        @Deprecated(NO_GETTER, level = ERROR) get
    var titleResource: Int
        @Deprecated(NO_GETTER, level = ERROR) get

    var message: CharSequence
        @Deprecated(NO_GETTER, level = ERROR) get
    var messageResource: Int
        @Deprecated(NO_GETTER, level = ERROR) get

    var icon: Drawable
        @Deprecated(NO_GETTER, level = ERROR) get
    var iconResource: Int
        @Deprecated(NO_GETTER, level = ERROR) get

    var customTitle: View
        @Deprecated(NO_GETTER, level = ERROR) get

    var customView: View
        @Deprecated(NO_GETTER, level = ERROR) get

    var isCancelable: Boolean
        @Deprecated(NO_GETTER, level = ERROR) get

    fun onCancelled(handler: (DialogInterface) -> Unit)

    fun onKeyPressed(handler: (DialogInterface, keyCode: Int, e: KeyEvent) -> Boolean)

    fun positiveButton(buttonText: String, onClicked: (dialog: DialogInterface) -> Unit)

    fun positiveButton(
        @StringRes buttonTextResource: Int,
        onClicked: (dialog: DialogInterface) -> Unit,
    )

    fun negativeButton(buttonText: String, onClicked: (dialog: DialogInterface) -> Unit)

    fun negativeButton(
        @StringRes buttonTextResource: Int,
        onClicked: (dialog: DialogInterface) -> Unit,
    )

    fun neutralPressed(buttonText: String, onClicked: (dialog: DialogInterface) -> Unit)

    fun neutralPressed(
        @StringRes buttonTextResource: Int,
        onClicked: (dialog: DialogInterface) -> Unit,
    )

    fun items(
        items: List<CharSequence>, onItemSelected: (dialog: DialogInterface, index: Int) -> Unit
    )

    fun singleChoiceItems(
        items: List<CharSequence>,
        checkedIndex: Int,
        onItemSelected: (dialog: DialogInterface, index: Int) -> Unit
    )

    fun multiChoiceItems(
        items: List<CharSequence>,
        checkedItems: BooleanArray,
        onItemSelected: (dialog: DialogInterface, index: Int, isChecked: Boolean) -> Unit,
    )

    fun build(): D

    fun show(): D
}

/**
 * 警报对话框生成器
 * 创建[AlertDialogBuilder]
 * @suppress Generate Documentation
 */
abstract class AlertDialogBuilder : AlertBuilder<AlertDialog> {

    abstract val builder: AlertDialog.Builder
    override val context: Context get() = builder.context

    override var title: CharSequence
        @Deprecated(NO_GETTER, level = ERROR) get() = noGetter()
        set(value) {
            builder.setTitle(value)
        }

    override var titleResource: Int
        @Deprecated(NO_GETTER, level = ERROR) get() = noGetter()
        set(value) {
            builder.setTitle(value)
        }

    override var message: CharSequence
        @Deprecated(NO_GETTER, level = ERROR) get() = noGetter()
        set(value) {
            builder.setMessage(value)
        }

    override var messageResource: Int
        @Deprecated(NO_GETTER, level = ERROR) get() = noGetter()
        set(value) {
            builder.setMessage(value)
        }

    override var icon: Drawable
        @Deprecated(NO_GETTER, level = ERROR) get() = noGetter()
        set(value) {
            builder.setIcon(value)
        }

    override var iconResource: Int
        @Deprecated(NO_GETTER, level = ERROR) get() = noGetter()
        set(value) {
            builder.setIcon(value)
        }

    override var customTitle: View
        @Deprecated(NO_GETTER, level = ERROR) get() = noGetter()
        set(value) {
            builder.setCustomTitle(value)
        }

    override var customView: View
        @Deprecated(NO_GETTER, level = ERROR) get() = noGetter()
        set(value) {
            builder.setView(value)
        }

    override var isCancelable: Boolean
        @Deprecated(NO_GETTER, level = ERROR) get() = noGetter()
        set(value) {
            builder.setCancelable(value)
        }

    override fun onCancelled(handler: (DialogInterface) -> Unit) {
        builder.setOnCancelListener(handler)
    }

    override fun onKeyPressed(handler: (DialogInterface, keyCode: Int, e: KeyEvent) -> Boolean) {
        builder.setOnKeyListener(handler)
    }

    override fun positiveButton(buttonText: String, onClicked: (DialogInterface) -> Unit) {
        builder.setPositiveButton(buttonText) { dialog, _ -> onClicked(dialog) }
    }

    override fun positiveButton(buttonTextResource: Int, onClicked: (DialogInterface) -> Unit) {
        builder.setPositiveButton(buttonTextResource) { dialog, _ -> onClicked(dialog) }
    }

    override fun negativeButton(buttonText: String, onClicked: (DialogInterface) -> Unit) {
        builder.setNegativeButton(buttonText) { dialog, _ -> onClicked(dialog) }
    }

    override fun negativeButton(buttonTextResource: Int, onClicked: (DialogInterface) -> Unit) {
        builder.setNegativeButton(buttonTextResource) { dialog, _ -> onClicked(dialog) }
    }

    override fun neutralPressed(buttonText: String, onClicked: (DialogInterface) -> Unit) {
        builder.setNeutralButton(buttonText) { dialog, _ -> onClicked(dialog) }
    }

    override fun neutralPressed(buttonTextResource: Int, onClicked: (DialogInterface) -> Unit) {
        builder.setNeutralButton(buttonTextResource) { dialog, _ -> onClicked(dialog) }
    }

    override fun items(items: List<CharSequence>, onItemSelected: (DialogInterface, Int) -> Unit) {
        builder.setItems(items.toTypedArray()) { dialog, which ->
            onItemSelected(dialog, which)
        }
    }

    override fun singleChoiceItems(
        items: List<CharSequence>, checkedIndex: Int, onItemSelected: (DialogInterface, Int) -> Unit
    ) {
        builder.setSingleChoiceItems(items.toTypedArray(), checkedIndex) { dialog, which ->
            onItemSelected(dialog, which)
        }
    }

    override fun multiChoiceItems(
        items: List<CharSequence>,
        checkedItems: BooleanArray,
        onItemSelected: (DialogInterface, Int, Boolean) -> Unit,
    ) {
        builder.setMultiChoiceItems(
            items.toTypedArray(), checkedItems
        ) { dialog, which, isChecked ->
            onItemSelected(dialog, which, isChecked)
        }
    }

    override fun build(): AlertDialog = builder.create()

    override fun show(): AlertDialog = builder.show()
}
