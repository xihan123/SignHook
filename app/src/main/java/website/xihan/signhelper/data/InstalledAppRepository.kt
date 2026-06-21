package website.xihan.signhelper.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.flow.Flow
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.roundToInt

class InstalledAppRepository(
    context: Context, private val dao: InstalledAppDao
) {

    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    fun observeApps(): Flow<List<InstalledAppEntity>> = dao.observeApps()

    suspend fun getFakeSignatureSettings(): List<PackageFakeSignatureSetting> =
        dao.getFakeSignatureSettings()

    suspend fun updateFakeSignature(packageName: String, fakeSignature: String) {
        dao.updateFakeSignatureValue(packageName, fakeSignature)
    }

    suspend fun updateFakeSignatureEnabled(packageName: String, enabled: Boolean) {
        dao.updateFakeSignatureEnabled(packageName, enabled)
    }

    suspend fun refreshInstalledApps() {
        val fakeSignatureSettings = dao.getFakeSignatureSettings().associateBy { it.packageName }
        val now = System.currentTimeMillis()

        val apps = getInstalledPackages().mapNotNull { packageInfo ->
                val applicationInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                val packageName = packageInfo.packageName
                val appName =
                    applicationInfo.loadLabel(packageManager).toString().ifBlank { packageName }
                val fakeSignatureSetting = fakeSignatureSettings[packageName]

                InstalledAppEntity(
                    packageName = packageName,
                    appName = appName,
                    versionName = packageInfo.versionName.orEmpty(),
                    versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
                    signatureValue = packageInfo.signatureSha256(),
                    fakeSignature = fakeSignatureSetting?.fakeSignature.orEmpty(),
                    fakeSignatureEnabled = fakeSignatureSetting?.fakeSignatureEnabled == true,
                    isSystemApp = applicationInfo.isSystemApp(),
                    iconPng = applicationInfo.loadIcon(packageManager).toPngBytes(),
                    updatedAt = now
                )
            }.sortedWith(compareBy<InstalledAppEntity> {
                it.appName.lowercase(Locale.getDefault())
            }.thenBy { it.packageName })

        dao.replaceInstalledApps(apps)
    }

    private fun getInstalledPackages(): List<PackageInfo> {
        val flags = PackageManager.GET_SIGNING_CERTIFICATES.toLong()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags))
        } else {
            @Suppress("DEPRECATION") packageManager.getInstalledPackages(PackageManager.GET_SIGNING_CERTIFICATES)
        }
    }

    private fun ApplicationInfo.isSystemApp(): Boolean {
        val systemFlags = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        return (flags and systemFlags) != 0
    }

    private fun PackageInfo.signatureSha256(): String {
        val signingInfo = signingInfo ?: return ""
        val signatures = if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }

        return signatures.orEmpty().joinToString(separator = "\n") { signature ->
                signature.toByteArray().toHexString()
            }
    }


    private fun Drawable.toPngBytes(): ByteArray? {
        val sourceWidth = intrinsicWidth.takeIf { it > 0 } ?: DEFAULT_ICON_SIZE
        val sourceHeight = intrinsicHeight.takeIf { it > 0 } ?: DEFAULT_ICON_SIZE
        val scale = minOf(
            1f,
            DEFAULT_ICON_SIZE.toFloat() / sourceWidth,
            DEFAULT_ICON_SIZE.toFloat() / sourceHeight
        )
        val width = (sourceWidth * scale).roundToInt().coerceAtLeast(1)
        val height = (sourceHeight * scale).roundToInt().coerceAtLeast(1)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val oldBounds = copyBounds()

        return try {
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                output.toByteArray()
            }
        } finally {
            bounds = oldBounds
            bitmap.recycle()
        }
    }

    private companion object {
        const val DEFAULT_ICON_SIZE = 128
    }
}
