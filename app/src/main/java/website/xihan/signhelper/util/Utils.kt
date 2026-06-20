package website.xihan.signhelper.util


import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import website.xihan.signhelper.R
import website.xihan.signhelper.ui.MainActivity
import kotlin.jvm.java
import kotlin.system.exitProcess

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 17:39
 * @介绍 :
 */

/*
/**
 * 一个简单的App图标
 */
@Preview(
    showBackground = false,
    backgroundColor = 0x00000000,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun AppIcon() {
    Box(
        modifier = Modifier
            .size(80.dp, 80.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(15.dp))
    ) {

        Text(
            text = "签名助手",
            modifier = Modifier
                .align(Alignment.Center),
            color = Color.Black,
            style = TextStyle(
                fontSize = 16.sp,
                letterSpacing = 0.15.sp
            ),
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}

 */

val kJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

/**
 * 设置竖向间隔dp
 * Spacer with vertical
 */
@Composable
fun VerticalSpace(dp: Int) {
    VerticalSpace(dp.dp)
}

@Composable
fun VerticalSpace(dp: Dp) {
    Spacer(Modifier.height(dp))
}

@Composable
fun <T> rememberMutableStateOf(value: T): MutableState<T> = remember { mutableStateOf(value) }

/**
 * 字符串复制到剪切板
 */
fun Context.copyToClipboard(content: String) {
    val clipboardManager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText(getString(R.string.clipboard_label_text), content)
    clipboardManager.setPrimaryClip(clipData)
}

fun Context.toast(message: CharSequence?): Toast =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).apply { show() }

/**
 * 隐藏应用图标
 */
fun Context.hideAppIcon() {
    val componentName = ComponentName(this, MainActivity::class.java.name)
    if (packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}

/**
 * 显示应用图标
 */
fun Context.showAppIcon() {
    val componentName = ComponentName(this, MainActivity::class.java.name)
    if (packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}

/**
 * 重启当前应用
 */
fun Activity.restartApplication() {
    // https://stackoverflow.com/a/58530756
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    finishAffinity()
    startActivity(intent)
    exitProcess(0)
}


fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

infix fun Int.x(other: Int): ViewGroup.LayoutParams = ViewGroup.LayoutParams(this, other)

inline fun <reified T> Any?.safeCast(): T? = this as? T
