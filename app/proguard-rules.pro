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
-printseeds seeds.txt
#列出从 apk 中删除的代码
-printusage unused.txt
#混淆前后的映射
-printmapping mapping.txt

#---------------------------------默认保留区---------------------------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View


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

-keep class com.hjq.permissions.** {*;}

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