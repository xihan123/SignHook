package cn.xihan.sign.utli

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
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
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cn.xihan.sign.BuildConfig
import cn.xihan.sign.R
import cn.xihan.sign.hook.HookEntry.Companion.optionModel
import cn.xihan.sign.model.ApkSignature
import cn.xihan.sign.model.ApkSignatureDao
import cn.xihan.sign.model.OptionModel
import cn.xihan.sign.ui.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.yukihookapi.hook.log.loggerE
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
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

fun String.loge() {
    if (BuildConfig.DEBUG) {
        loggerE(msg = this)
    }
}

/**
 * 获取指定包名的签名信息
 */
fun Context.getSignature(pkgName: String): String = runCatching {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageManager.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
    } else {
        packageManager.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES)
    }
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.signingInfo?.apkContentsSigners
    } else {
        packageInfo?.signatures
    }
    signatures?.firstOrNull()?.toCharsString() ?: ""
}.getOrElse {
    ""
}

fun Context.getApkSignature(file: File): String? = runCatching {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageManager.getPackageArchiveInfo(
            file.absolutePath,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
    } else {
        packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
    }
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.signingInfo?.apkContentsSigners
    } else {
        packageInfo?.signatures
    }
    signatures?.first()?.toCharsString()
}.getOrElse { "" }

/**
 * 读取配置
 */
fun readConfigFile(): File? = try {
    File(
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/SignHelper",
        "option.json"
    ).also {
        if (!it.exists()) {
            it.parentFile?.mkdirs()
            it.createNewFile()
        }
    }
} catch (e: Throwable) {
    "readConfigFile: ${e.message}".loge()
    null
}

/**
 * 读取配置模型
 */
fun readOptionModel(): OptionModel {
    val file = readConfigFile() ?: return OptionModel()
    return try {
        if (file.readText().isNotEmpty()) {
            try {
                val kJson = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                kJson.decodeFromString<OptionModel>(file.readText()).apply {
                    val newOptionModel = OptionModel()
                    val packageNameList = this.packageNameList.toMutableList()
                    newOptionModel.packageNameList.forEach { packageName ->
                        if (!packageNameList.any { it == packageName }) {
                            packageNameList.plusAssign(packageName)
                        }
                    }
                    this.packageNameList = packageNameList

                }
            } catch (e: Exception) {
                loggerE(msg = "readOptionFile: ${e.message}")
                OptionModel()
            }
        } else {
            OptionModel()
        }
    } catch (e: Exception) {
        loggerE(msg = "readOptionModel: ${e.message}")
        OptionModel()
    }

}

/**
 * 写入配置文件
 */
fun writeConfigFile(optionModel: OptionModel): Boolean =
    try {
        readConfigFile()?.writeText(Json.encodeToString(optionModel))
        true
    } catch (e: Throwable) {
        "writeConfigFile: ${e.message}".loge()
        false
    }

fun writeConfigFile() = try {
    readConfigFile()?.writeText(Json.encodeToString(optionModel))
    true
} catch (e: Throwable) {
    "writeConfigFile: ${e.message}".loge()
    false
}

/**
 * 字符串复制到剪切板
 */
fun Context.copyToClipboard(content: String) {
    val clipboardManager =
        this.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clipData = android.content.ClipData.newPlainText("text", content)
    clipboardManager.setPrimaryClip(clipData)
}

/**
 * 请求权限
 */
fun Context.requestPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    if (this.applicationInfo.targetSdkVersion > 30) {
        XXPermissions.with(this)
            .permission(Permission.MANAGE_EXTERNAL_STORAGE, Permission.REQUEST_INSTALL_PACKAGES)
            .request { _, allGranted ->
                if (allGranted) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
    } else {
        XXPermissions.with(this)
            .permission(Permission.Group.STORAGE.plus(Permission.REQUEST_INSTALL_PACKAGES))
            .request { _, allGranted ->
                if (allGranted) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
    }
}

/**
 * 跳转获取权限
 */
fun Context.jumpToPermission() {
    if (this.applicationInfo.targetSdkVersion > 30) {
        XXPermissions.startPermissionActivity(this, Permission.MANAGE_EXTERNAL_STORAGE)
    } else {
        XXPermissions.startPermissionActivity(this, Permission.Group.STORAGE)
    }
}

/**
 * 解析词组
 */
fun String.parseKeyWordOption(): MutableSet<String> =
    this.split(";").filter { it.isNotBlank() }.map { it.replace(Regex(pattern = "\\s+"), "") }
        .toMutableSet()

/**
 * 弹框提示签名值 并且复制到剪切板
 */
fun Context.showSignatureDialog(signature: String) {
    // 创建一个MaterialAlertDialogBuilder对象
    MaterialAlertDialogBuilder(this).apply {
        setTitle(getString(R.string.title_original_signature))
        setMessage(signature)
        setPositiveButton(android.R.string.copy) { dialog, which ->
            copyToClipboard(signature)
            toast("已复制到剪切板")
            dialog.dismiss()
        }
        show()
    }
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

@Database(
    entities = [ApkSignature::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun apkSignatureDao(): ApkSignatureDao

    companion object {
        private const val DATABASE_NAME = "sign.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Synchronized
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .createFromAsset("database/sign.db")
                .build()
        }

    }

}