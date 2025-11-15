package website.xihan.signhelper.util

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener

/**
 * 自定义线性布局
 * 创建[CustomLinearLayout]
 * @param [context] 上下文
 * @param [isAutoWidth] 是自动宽度
 * @param [isAutoHeight] 是自动高度
 * @suppress Generate Documentation
 */
open class CustomLinearLayout(
    context: Context,
    isAutoWidth: Boolean = true,
    isAutoHeight: Boolean = true,
) : LinearLayout(context) {

    private val linearLayout by lazy {
        LinearLayout(context).apply {
            layoutParams =
                if (isAutoWidth) WRAP_CONTENT x WRAP_CONTENT else MATCH_PARENT x MATCH_PARENT
            orientation = VERTICAL
            setPadding(context.dp(10), context.dp(10), context.dp(10), context.dp(10))
        }
    }

    private val scrollView by lazy {
        ScrollView(context).apply {
            layoutParams =
                if (isAutoWidth) WRAP_CONTENT x WRAP_CONTENT else MATCH_PARENT x MATCH_PARENT
            isFillViewport = true
            addView(linearLayout)
        }
    }

    /**
     * 设定自定义 LinearLayout 的布局参数
     */
    init {
        apply {
            layoutParams =
                if (isAutoWidth) WRAP_CONTENT x WRAP_CONTENT else MATCH_PARENT x MATCH_PARENT
            orientation = VERTICAL
            setPadding(context.dp(10), context.dp(10), context.dp(10), context.dp(10))
            addView(scrollView, 0)
        }
    }

    override fun addView(child: View?) {
        linearLayout.addView(child)
    }


}

/**
 * 自定义编辑文本
 * 创建[CustomEditText]
 * @param [context] 上下文
 * @param [title] 标题
 * @param [message] 消息
 * @param [value] 价值
 * @param [mHint] m提示
 * @param [isAvailable] 可用
 * @param [isAutoWidth] 是自动宽度
 * @param [isAutoHeight] 是自动高度
 * @param [block] 块
 * @suppress Generate Documentation
 */
open class CustomEditText(
    context: Context,
    title: String = "",
    message: String = "",
    value: String? = "",
    hint: String = "",
    isAvailable: Boolean = true,
    isAutoWidth: Boolean = false,
    isAutoHeight: Boolean = true,
    block: (String) -> Unit = {},
) : CustomLinearLayout(context, isAutoWidth, isAutoHeight) {

    val editText by lazy {
        EditText(context).apply {
            isEnabled = isAvailable
             layoutParams =
                if (isAutoWidth) WRAP_CONTENT x WRAP_CONTENT else MATCH_PARENT x MATCH_PARENT
            when {
                value.isNullOrBlank() && hint.isNotBlank() -> this.hint = hint
                else -> setText(value)
            }
            addTextChangedListener {
                block(it.toString())
            }
        }
    }

    val tvTitle by lazy {
        TextView(context).apply {
            text = title
            textSize = 16F
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    val tvSubTitle by lazy {
        TextView(context).apply {
            text = message
            textSize = 12F
        }
    }

    init {
        apply {
            addView(tvTitle)
            addView(tvSubTitle)
            addView(editText)
            setPadding(context.dp(10), context.dp(10), context.dp(10), context.dp(10))
        }
    }

}

/**
 * 自定义文本视图
 * 创建[CustomTextView]
 * @param [context] 上下文
 * @param [text] 文本
 * @param [textSize] 文本大小
 * @param [isBold] 是粗体
 * @param [isAutoWidth] 是自动宽度
 * @param [isAutoHeight] 是自动高度
 * @param [onClickAction] 点击操作
 * @suppress Generate Documentation
 */
class CustomTextView(
    context: Context,
    text: CharSequence = "",
    textSize: Float = 16F,
    isBold: Boolean = false,
    isAutoWidth: Boolean = false,
    isAutoHeight: Boolean = true,
    onClickAction: () -> Unit = {},
) : CustomLinearLayout(context, isAutoWidth, isAutoHeight) {

    val textView by lazy {
        TextView(context).apply {
            this.text = text
            this.textSize = textSize
            if (isBold) {
                setTypeface(typeface, Typeface.BOLD)
            }
            setOnClickListener { onClickAction() }
            setOnLongClickListener {
                context.apply {
                    copyToClipboard(text.toString())
                    toast("已复制到剪贴板")
                }
                true
            }

        }
    }

    init {
        apply {
            addView(textView)
            layoutParams =
                if (isAutoWidth) WRAP_CONTENT x WRAP_CONTENT else MATCH_PARENT x MATCH_PARENT
            setPadding(context.dp(10), context.dp(10), context.dp(10), context.dp(10))
        }
    }
}
