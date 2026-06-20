package website.xihan.signhelper.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import website.xihan.signhelper.base.BaseDao

@Dao
interface InstalledAppDao : BaseDao<InstalledAppEntity> {

    @Query(
        """
        SELECT * FROM installed_apps
        ORDER BY
            CASE WHEN fakeSignatureEnabled = 1 AND fakeSignature != '' THEN 0 ELSE 1 END,
            appName COLLATE NOCASE,
            packageName COLLATE NOCASE
        """
    )
    fun observeApps(): Flow<List<InstalledAppEntity>>

    @Query("SELECT packageName, fakeSignature, fakeSignatureEnabled FROM installed_apps")
    suspend fun getFakeSignatureSettings(): List<PackageFakeSignatureSetting>

    @Query("UPDATE installed_apps SET fakeSignature = :fakeSignature WHERE packageName = :packageName")
    suspend fun updateFakeSignature(packageName: String, fakeSignature: String)

    @Query("UPDATE installed_apps SET fakeSignatureEnabled = :enabled WHERE packageName = :packageName")
    suspend fun updateFakeSignatureEnabled(packageName: String, enabled: Boolean)

    @Query("UPDATE installed_apps SET fakeSignatureEnabled = 0 WHERE packageName = :packageName")
    suspend fun disableFakeSignature(packageName: String)

    @Transaction
    suspend fun updateFakeSignatureValue(packageName: String, fakeSignature: String) {
        updateFakeSignature(packageName, fakeSignature)
        if (fakeSignature.isBlank()) {
            disableFakeSignature(packageName)
        }
    }

    @Query("DELETE FROM installed_apps WHERE packageName NOT IN (:packageNames)")
    suspend fun deleteMissingPackages(packageNames: List<String>)

    @Query("DELETE FROM installed_apps")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceInstalledApps(apps: List<InstalledAppEntity>) {
        if (apps.isEmpty()) {
            deleteAll()
            return
        }

        upsert(apps)
        deleteMissingPackages(apps.map { it.packageName })
    }
}
