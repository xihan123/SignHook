package cn.xihan.signhook.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class TitleAndPackageNameModel(
    @SerialName("packageName")
    var packageName: String = "",
    @SerialName("title")
    var title: String = ""
)

@Keep
@Serializable
data class PackageNameAndSignatureModel(
    @SerialName("packageName")
    var packageName: String = "",
    @SerialName("signatureValue")
    var signatureValue: String = ""
)