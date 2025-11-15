#---------------------------------基本指令区---------------------------------
# 指定代码的压缩级别
-optimizationpasses 7
-flattenpackagehierarchy
-allowaccessmodification
# 避免混淆Annotation、内部类、泛型、匿名类
-keepattributes Signature,Exceptions,*Annotation*,
                InnerClasses,PermittedSubclasses,EnclosingMethod,
                Deprecated,SourceFile,LineNumberTable
-keepattributes 'SourceFile'
-renamesourcefileattribute '希涵'
-obfuscationdictionary 'dictionary.txt'
-classobfuscationdictionary 'dictionary.txt'
-packageobfuscationdictionary 'dictionary.txt'
#混淆时不使用大小写混合，混淆后的类名为小写(大小写混淆容易导致class文件相互覆盖）
-dontusemixedcaseclassnames
#未混淆的类和成员
-printseeds proguard/seeds.txt
#列出从 apk 中删除的代码
-printusage proguard/unused.txt
#混淆前后的映射
-printmapping proguard/mapping.txt

#---------------------------------默认保留区---------------------------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keepclasseswithmembers class * {
    @androidx.annotation.* <fields>;
}
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
# Xposed API
-keep class de.robv.android.xposed.** { *; }
-keep public class * implements de.robv.android.xposed.IXposedHookLoadPackage
-keep class website.xihan.signhelper.hook.HookEntry { *; }
-dontwarn de.robv.android.xposed.**
-dontwarn io.github.libxposed.api.**

# Keep Kotlin reflection metadata
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keep class kotlin.jvm.internal.** { *; }

# Keep Kotlin companion objects
-keepclassmembers class * {
    static final *** Companion;
    static final *** INSTANCE;
}

# 保留资源绑定类（如果有）
-keep class **.*R$* { *; }
-keep class **.R$* { *; }

# AlertDialog
-keepclassmembers class androidx.appcompat.app.AlertDialog {
    *** mAlert;
}

-keepclassmembers class androidx.appcompat.app.AlertController {
    *** mButtonPositive;
    *** mButtonNegative;
    *** mButtonNeutral;
}

# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Remove Kotlin Instrisics (should not impact the app)
# https://proandroiddev.com/is-your-kotlin-code-really-obfuscated-a36abf033dde
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkExpressionValueIsNotNull(...);
    public static void checkFieldIsNotNull(...);
    public static void checkFieldIsNotNull(...);
    public static void checkNotNull(...);
    public static void checkNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkNotNullParameter(...);
    public static void checkParameterIsNotNull(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void throwUninitializedPropertyAccessException(...);
}

# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**


# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-keepclassmembers class androidx.compose.ui.graphics.AndroidImageBitmap_androidKt{
public *** asImageBitmap(...);
}
-keepclassmembers class androidx.compose.ui.platform.AndroidCompositionLocals_androidKt{
public *** getLocalContext(...);
}
-keepclassmembers class androidx.compose.foundation.OverscrollConfigurationKt{
public *** getLocalOverscrollConfiguration(...);
}
#---------------------------------序列化指令区---------------------------------
-dontnote kotlinx.serialization.SerializersKt
-keep,includedescriptorclasses class cn.xihan.sign.**$$serializer { *; }
-keepclassmembers class cn.xihan.sign.** {
    *** Companion;
}
-keepclasseswithmembers class cn.xihan.sign.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn java.lang.ClassValue
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE