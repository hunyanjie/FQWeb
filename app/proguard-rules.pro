# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# 混合时不使用大小写混合，混合后的类名为小写
-dontusemixedcaseclassnames

# 指定不去忽略非公共库的类
-dontskipnonpubliclibraryclasses

# 这句话能够使我们的项目混淆后产生映射文件
# 包含有类名->混淆后类名的映射关系
-verbose

# 指定不去忽略非公共库的类成员
-dontskipnonpubliclibraryclassmembers

# 不做预校验，preverify是proguard的四个步骤之一，Android不需要preverify，去掉这一步能够加快混淆速度。
-dontpreverify

# 保留Annotation不混淆
-keepattributes *Annotation*,InnerClasses

# 避免混淆泛型
-keepattributes Signature

# 抛出异常时保留代码行号
-keepattributes SourceFile,LineNumberTable

# 指定混淆是采用的算法，后面的参数是一个过滤器
# 这个过滤器是谷歌推荐的算法，一般不做更改
-optimizations !code/simplification/cast,!field/*,!class/merging/*

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

# Error Exception
-keep class * extends java.lang.Exception
-keep class * extends java.lang.Error
-keep class **Exception

# 保留
-keep class me.fycz.fqweb.MainHook
-keepclassmembers class me.fycz.fqweb.web.ReturnData {<fields>;}
-keepclassmembers class me.fycz.fqweb.entity.NATTraversalConfig {<fields>;<init>();}
-keepclassmembers class me.fycz.fqweb.entity.ServerConfig {<fields>;<init>();}
