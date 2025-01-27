package cn.xihan.signhook.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/26 15:15
 * @介绍 :
 */
@Serializable
data class SaResult<T>(
    @SerialName("code") val code: Int = 0,
    @SerialName("msg") val message: String = "",
    @SerialName("data") val data: T? = null
)

fun SaResult<*>.isSuccess() = code == 200 || data != null