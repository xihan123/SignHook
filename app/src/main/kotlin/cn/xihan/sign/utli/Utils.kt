package cn.xihan.sign.utli

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.DisplayMetrics
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
import cn.xihan.sign.model.ApkSignature
import cn.xihan.sign.model.ApkSignatureDao
import cn.xihan.sign.ui.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.yukihookapi.hook.log.YLog
import java.io.File
import java.lang.Boolean
import java.lang.reflect.Constructor
import kotlin.Any
import kotlin.Array
import kotlin.CharSequence
import kotlin.Int
import kotlin.String
import kotlin.also
import kotlin.apply
import kotlin.arrayOfNulls
import kotlin.getOrElse
import kotlin.let
import kotlin.runCatching
import kotlin.stackTraceToString
import kotlin.synchronized
import kotlin.system.exitProcess

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 17:39
 * @介绍 :
 *//*
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
        YLog.error(msg = this, tag = YLog.Configs.tag)
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

@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
fun getApkRawSignatures(apkPath: String): String = runCatching {
    val pathPackageParser = "android.content.pm.PackageParser"
    val parsePackage = "parsePackage"
    val collectCertificates = "collectCertificates"
    val fieldMSignatures = "mSignatures"
    val fieldMSigningDetails = "mSigningDetails"
    val fieldSignatures = "signatures"

    val pkgParserCls = Class.forName(pathPackageParser)
    val typeArgs: Array<Class<*>?> = arrayOfNulls(1)
    typeArgs[0] = String::class.java
    val metrics = DisplayMetrics()
    metrics.setToDefaults()
    val pkgParser: Any
    val pkgParserCt: Constructor<*> = pkgParserCls.getConstructor()
    pkgParser = pkgParserCt.newInstance()
    val pkgParserParsePackageMtd =
        pkgParserCls.getDeclaredMethod(parsePackage, File::class.java, Int::class.javaPrimitiveType)
    val pkgParserPkg =
        pkgParserParsePackageMtd.invoke(pkgParser, File(apkPath), PackageManager.GET_SIGNATURES)
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val pkgParserCollectCertificatesMtd = pkgParserCls.getDeclaredMethod(
            collectCertificates, pkgParserPkg.javaClass, Boolean.TYPE
        )
        pkgParserCollectCertificatesMtd.invoke(
            pkgParser, pkgParserPkg, Build.VERSION.SDK_INT > Build.VERSION_CODES.P
        )
        val mSigningDetailsField = pkgParserPkg.javaClass.getDeclaredField(fieldMSigningDetails)
        mSigningDetailsField.isAccessible = true
        val mSigningDetails = mSigningDetailsField[pkgParserPkg]
        val packageInfoField = mSigningDetails.javaClass.getDeclaredField(fieldSignatures)
        packageInfoField.isAccessible = true
        packageInfoField[mSigningDetails]
    } else {
        val pkgParserCCerMtd = pkgParserCls.getDeclaredMethod(
            collectCertificates, pkgParserPkg.javaClass, Integer.TYPE
        )
        pkgParserCCerMtd.invoke(pkgParser, pkgParserPkg, PackageManager.GET_SIGNATURES)
        val packageInfoField = pkgParserPkg.javaClass.getDeclaredField(fieldMSignatures)
        packageInfoField[pkgParserPkg]
    }
    signatures?.let { apkRawSignatures ->
        if (apkRawSignatures is Array<*> && apkRawSignatures.isArrayOf<Signature>())
            (apkRawSignatures.first() as Signature).toCharsString() else "ApkRawSignaturesNotArray"
    } ?: "ApkRawSignaturesNUll"
}.getOrElse { throwable -> throwable.stackTraceToString() }

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

/**
 * 默认作用域包名Set列表
 */
val defaultScopeSet = setOf("com.tencent.mm", "com.tencent.mobileqq", "com.ss.android.ugc.aweme")

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
                .fallbackToDestructiveMigration().createFromAsset("database/sign.db").build()
        }
    }
}