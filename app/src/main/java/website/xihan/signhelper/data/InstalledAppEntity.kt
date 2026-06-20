package website.xihan.signhelper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_apps")
data class InstalledAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val signatureValue: String,
    val fakeSignature: String,
    val fakeSignatureEnabled: Boolean,
    val isSystemApp: Boolean,
    val iconPng: ByteArray?,
    val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InstalledAppEntity

        if (versionCode != other.versionCode) return false
        if (fakeSignatureEnabled != other.fakeSignatureEnabled) return false
        if (isSystemApp != other.isSystemApp) return false
        if (updatedAt != other.updatedAt) return false
        if (packageName != other.packageName) return false
        if (appName != other.appName) return false
        if (versionName != other.versionName) return false
        if (signatureValue != other.signatureValue) return false
        if (fakeSignature != other.fakeSignature) return false
        if (!iconPng.contentEquals(other.iconPng)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = versionCode.hashCode()
        result = 31 * result + fakeSignatureEnabled.hashCode()
        result = 31 * result + isSystemApp.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + appName.hashCode()
        result = 31 * result + versionName.hashCode()
        result = 31 * result + signatureValue.hashCode()
        result = 31 * result + fakeSignature.hashCode()
        result = 31 * result + (iconPng?.contentHashCode() ?: 0)
        return result
    }
}

data class PackageFakeSignatureSetting(
    val packageName: String, val fakeSignature: String, val fakeSignatureEnabled: Boolean
)
