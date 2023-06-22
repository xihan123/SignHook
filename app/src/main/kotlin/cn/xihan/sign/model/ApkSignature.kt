package cn.xihan.sign.model

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import cn.xihan.sign.base.BaseDao
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 19:37
 * @介绍 :
 */
/**
 * @param apkName 应用名
 * @param packageName 包名
 * @param originalSignature 原始签名
 * @param forgedSignature 伪造签名
 * @param isForged 是否伪造
 */
@Keep
@Serializable
@Parcelize
@Entity
data class ApkSignature(
    var apkName: String = "",
    @PrimaryKey
    var packageName: String = "",
    var originalSignature: String = "",
    var forgedSignature: String = "",
    var isForged: Boolean = false
) : Parcelable


/**
 * 选择模型
 * @author MissYang
 * @date 2023/06/22
 * @param [hideIcon] 是否隐藏图标
 * @param [packageNameList] 包名称列表
 * @param [apkSignatureList] apk签名列表
 */
@Keep
@Serializable
@Parcelize
data class OptionModel(
    var hideIcon: Boolean = false,
    var packageNameList: List<String> = listOf(
        "com.tencent.mm",
        "com.tencent.mobileqq",
        "com.ss.android.ugc.aweme"
    ),
    var apkSignatureList: List<ApkSignature> = emptyList()
) : Parcelable

@Dao
interface ApkSignatureDao : BaseDao<ApkSignature> {

    /**
     * 根据包名查询
     * @param packageName 包名
     * @return 查询结果
     */
    @Query("SELECT * FROM ApkSignature WHERE packageName = :packageName")
    suspend fun queryApkSignature(packageName: String): ApkSignature?

    /**
     * 更新 apkName、packageName、originalSignature
     */
    @Query("UPDATE ApkSignature SET apkName = :apkName, packageName = :packageName, originalSignature = :originalSignature WHERE packageName = :packageName")
    suspend fun updateApkSignature(apkName: String, packageName: String, originalSignature: String)

    /**
     * 根据 包名 更新 isForged
     */
    @Query("UPDATE ApkSignature SET isForged = :isForged WHERE packageName = :packageName")
    suspend fun updateIsForged(packageName: String, isForged: Boolean)

    /**
     * 根据 包名 更新 forgedSignature
     */
    @Query("UPDATE ApkSignature SET forgedSignature = :forgedSignature WHERE packageName = :packageName")
    suspend fun updateForgedSignature(packageName: String, forgedSignature: String)

    /**
     * 根据 包名 或者 应用名 模糊查询 isForged 为 TRUE 排最前 返回paging
     * @param query 查询条件
     * @return 查询结果
     */
    @Query("SELECT * FROM ApkSignature WHERE packageName LIKE '%' || :query || '%' OR apkName LIKE '%' || :query || '%' ORDER BY isForged DESC")
    fun queryAppInfoModel(query: String): PagingSource<Int, ApkSignature>

    /**
     * 查询全部条目并以forgedSignature不为空的在前排序返回分页paging
     */
    @Query("SELECT * FROM ApkSignature ORDER BY isForged DESC")
    fun queryAllAppInfoModelOrderByForgedSignature(): PagingSource<Int, ApkSignature>

    /**
     * 查询 forgedSignature 不为空 以及 isForged 为 true 的数据
     */
    @Query("SELECT * FROM ApkSignature WHERE forgedSignature != '' AND isForged = 1")
    fun queryAllAppInfoModelForged(): Flow<List<ApkSignature>>

}