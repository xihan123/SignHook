package cn.xihan.signhook.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/26 15:16
 * @介绍 :
 */
@Serializable
data class LoginRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String
)