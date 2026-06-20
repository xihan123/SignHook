package website.xihan.signhelper.hook

import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.content.pm.SigningInfo
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import website.xihan.signhelper.util.ALog
import website.xihan.signhelper.util.FakeSignatureConfig
import website.xihan.signhelper.util.Const.REMOTE_PREFS_GROUP
import website.xihan.signhelper.util.ReadOnlySharedPreferences
import website.xihan.signhelper.util.asReadOnly
import website.xihan.signhelper.util.enabledFakeSignature
import website.xihan.signhelper.util.safeCast
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.security.MessageDigest

class ModuleMain : XposedModule() {
    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
//        ALog.d("param.packageName: ${param.packageName}")
        val packageManagerClass =
            Class.forName("android.app.ApplicationPackageManager", true, param.classLoader)

        packageManagerClass.declaredMethods.filter(::isPackageInfoLookup)
            .forEach { method ->
                hook(method).intercept { chain ->
                    val requestedPackageName = chain.getArg(0).requestedPackageName()
                    val packageInfo = chain.proceed().safeCast<PackageInfo>()
                    val fakeSignatures = requestedPackageName.fakeSignaturesOrNull()
                    if (packageInfo != null && fakeSignatures != null) {
                        packageInfo.replaceSignatures(fakeSignatures)
                    }
                    packageInfo
                }
            }

        packageManagerClass.declaredMethods.filter(::isHasSigningCertificate)
            .forEach { method ->
                hook(method).intercept { chain ->
                    val requestedPackageName = chain.getArg(0).safeCast<String>()
                    val fakeSignatures = requestedPackageName.fakeSignaturesOrNull()
                        ?: return@intercept chain.proceed()

                    val certificate = chain.getArg(1).safeCast<ByteArray>()
                    val type = chain.getArg(2).safeCast<Int>()
                    if (certificate == null || type == null) {
                        return@intercept chain.proceed()
                    }

                    fakeSignatures.any { it.matchesCertificate(certificate, type) }
                }
            }
    }

    private fun String?.fakeSignaturesOrNull(): Array<Signature>? {
        val packageName = this ?: return null
        val config = readOnlyPreferences.enabledFakeSignature(packageName) ?: return null
//        ALog.d("fakeSignature[$packageName]: ${config.value}")
        return config.toSignatures()
    }

    private fun isPackageInfoLookup(method: Method): Boolean {
        val firstParameter = method.parameterTypes.firstOrNull() ?: return false
        return method.returnType == PackageInfo::class.java &&
                method.name in PACKAGE_INFO_METHODS &&
                firstParameter.isPackageNameLike()
    }

    private fun isHasSigningCertificate(method: Method): Boolean =
        method.name == "hasSigningCertificate" &&
                method.parameterTypes.size == 3 &&
                method.parameterTypes[0] == String::class.java &&
                method.parameterTypes[1] == ByteArray::class.java &&
                method.parameterTypes[2] == Int::class.javaPrimitiveType

    private fun Class<*>.isPackageNameLike(): Boolean =
        this == String::class.java || name == "android.content.pm.VersionedPackage"

    private fun Any?.requestedPackageName(): String? = when (this) {
        is String -> this
        null -> null
        else -> runCatching {
            javaClass.getMethod("getPackageName").invoke(this).safeCast<String>()
        }.getOrNull()
    }

    private fun FakeSignatureConfig.toSignatures(): Array<Signature>? =
        runCatching {
            value.lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map(::Signature)
                .toList()
                .takeIf { it.isNotEmpty() }
                ?.toTypedArray()
        }.onFailure(ALog::e).getOrNull()

    @Suppress("DEPRECATION")
    private fun PackageInfo.replaceSignatures(signatures: Array<Signature>) {
        this.signatures = signatures
        signatures.toSigningInfo()?.let { signingInfo = it }
    }

    private fun Array<Signature>.toSigningInfo(): SigningInfo? =
        runCatching {
            val signingDetailsClass = findSigningDetailsClass()
            val signingDetails = signingDetailsClass.createSigningDetails(this) ?: return null
            val constructor = SigningInfo::class.java.declaredConstructors.firstOrNull {
                it.parameterTypes.singleOrNull() == signingDetailsClass
            } ?: return null
            constructor.isAccessible = true
            constructor.newInstance(signingDetails).safeCast<SigningInfo>()
        }.onFailure(ALog::e).getOrNull()

    private fun findSigningDetailsClass(): Class<*> =
        runCatching { Class.forName("android.content.pm.SigningDetails") }
            .getOrElse { Class.forName("android.content.pm.PackageParser\$SigningDetails") }

    private fun Class<*>.createSigningDetails(signatures: Array<Signature>): Any? {
        val keys = runCatching { signatures.toPublicKeySet() }.getOrNull()
        val candidates = declaredConstructors.sortedByDescending { it.parameterTypes.size }

        return candidates.firstNotNullOfOrNull { constructor ->
            runCatching {
                constructor.newSigningDetails(signatures, keys)
            }.getOrNull()
        }
    }

    private fun Array<Signature>.toPublicKeySet(): Any {
        val arraySetClass = Class.forName("android.util.ArraySet")
        val keys = arraySetClass.getConstructor().newInstance()
        val add = arraySetClass.getMethod("add", Any::class.java)
        val getPublicKey = Signature::class.java.getMethod("getPublicKey")
        forEach { signature ->
            add.invoke(keys, getPublicKey.invoke(signature))
        }
        return keys
    }

    private fun Constructor<*>.newSigningDetails(
        signatures: Array<Signature>,
        keys: Any?
    ): Any? {
        val signatureArrayClass = signatures.javaClass
        val parameterTypes = this.parameterTypes
        if (parameterTypes.firstOrNull() != signatureArrayClass ||
            parameterTypes.getOrNull(1) != Int::class.javaPrimitiveType
        ) {
            return null
        }
        if (keys == null && parameterTypes.size > 2) {
            return null
        }

        val args: Array<Any?> = when (parameterTypes.size) {
            5 if parameterTypes[2] == Int::class.javaPrimitiveType -> {
                arrayOf(signatures, SIGNATURE_SCHEME_VERSION, SIGNATURE_SCHEME_MINOR_VERSION, keys, null)
            }
            5 -> {
                arrayOf(signatures, SIGNATURE_SCHEME_VERSION, keys, null, null)
            }
            4 -> {
                arrayOf(signatures, SIGNATURE_SCHEME_VERSION, keys, null)
            }
            2 -> {
                arrayOf(signatures, SIGNATURE_SCHEME_VERSION)
            }
            else -> return null
        }

        isAccessible = true
        return newInstance(*args)
    }

    private fun Signature.matchesCertificate(certificate: ByteArray, type: Int): Boolean =
        when (type) {
            PackageManager.CERT_INPUT_RAW_X509 -> toByteArray().contentEquals(certificate)
            PackageManager.CERT_INPUT_SHA256 -> sha256().contentEquals(certificate)
            else -> false
        }

    private fun Signature.sha256(): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(toByteArray())

    private val readOnlyPreferences: ReadOnlySharedPreferences
        get() = getRemotePreferences(REMOTE_PREFS_GROUP).asReadOnly()

    private companion object {
        val PACKAGE_INFO_METHODS = setOf("getPackageInfo", "getPackageInfoAsUser")
        const val SIGNATURE_SCHEME_VERSION = 2
        const val SIGNATURE_SCHEME_MINOR_VERSION = 0
    }
}
